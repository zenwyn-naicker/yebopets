/**************************************************************************************************************
 * Author: MongoDB, Inc.
 * Date: 2026
 * Title: Transactions and partial unique indexes
 * Type: [Source Code]
 * Available at: <https://www.mongodb.com/docs/drivers/csharp/current/crud/transactions/> [Accessed 22 October 2026]
 * Available at: <https://www.mongodb.com/docs/manual/core/index-partial/> [Accessed 22 October 2026]
 **************************************************************************************************************/
using MongoDB.Driver;

namespace YeboPets.Api;

public sealed class JourneyConflict(string code, string message) : Exception(message)
{
    public ApiError Error => new(code, Message);
}

/// <summary>Database constraints arbitrate concurrent requests; finalisation commits all records together.</summary>
public sealed class JourneyStore
{
    private static readonly string[] ActiveStatuses = ["Submitted", "UnderReview", "MoreInfo", "Approved"];
    private readonly Db db;
    private readonly IMongoCollection<Entry<ApplicationRecord>> applications;
    private readonly IMongoCollection<Entry<AdoptionRecord>> adoptions;
    private readonly IMongoCollection<Entry<Animal>> animals;
    private readonly SemaphoreSlim indexLock = new(1, 1);
    private bool indexesReady;

    public JourneyStore(Db db)
    {
        this.db = db;
        applications = db.Database.GetCollection<Entry<ApplicationRecord>>("applications");
        adoptions = db.Database.GetCollection<Entry<AdoptionRecord>>("adoptions");
        animals = db.Database.GetCollection<Entry<Animal>>("animals");
    }

    private async Task EnsureIndexes()
    {
        await indexLock.WaitAsync();
        try
        {
            if (indexesReady) return;
            await CreateIndexes();
            indexesReady = true;
        }
        finally { indexLock.Release(); }
    }

    private async Task CreateIndexes()
    {
        // Existing duplicates intentionally fail index creation instead of silently deleting assessment data.
        await applications.Indexes.CreateOneAsync(new CreateIndexModel<Entry<ApplicationRecord>>(
            Builders<Entry<ApplicationRecord>>.IndexKeys.Ascending(x => x.Value.AdopterId).Ascending(x => x.Value.AnimalId),
            new CreateIndexOptions<Entry<ApplicationRecord>>
            {
                Name = "one_active_application_per_adopter_pet", Unique = true,
                PartialFilterExpression = Builders<Entry<ApplicationRecord>>.Filter.In(x => x.Value.Status, ActiveStatuses)
            }));
        await adoptions.Indexes.CreateManyAsync([
            new(Builders<Entry<AdoptionRecord>>.IndexKeys.Ascending(x => x.Value.ApplicationId),
                new CreateIndexOptions { Name = "one_adoption_per_application", Unique = true }),
            new(Builders<Entry<AdoptionRecord>>.IndexKeys.Ascending(x => x.Value.AnimalId),
                new CreateIndexOptions { Name = "one_adoption_per_animal", Unique = true })
        ]);
    }

    public async Task Submit(ApplicationRecord application)
    {
        await EnsureIndexes();
        try
        {
            await applications.InsertOneAsync(new Entry<ApplicationRecord> { Id = application.Id, Value = application });
        }
        catch (MongoWriteException e) when (e.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            throw new JourneyConflict("duplicate_application", "You already have an active application for this pet.");
        }
    }

    public async Task<ApplicationRecord> Decide(ApplicationRecord application, ApplicationDecisionRequest request)
    {
        await EnsureIndexes();
        using var session = await db.Database.Client.StartSessionAsync();
        try
        {
            return await session.WithTransactionAsync(async (s, ct) =>
            {
                if (await adoptions.Find(s, x => x.Value.ApplicationId == application.Id).AnyAsync(ct))
                    throw new JourneyConflict("already_adopted", "This adoption has already been finalised.");
                var current = await applications.Find(s, x => x.Id == application.Id).FirstAsync(ct);
                var updated = current.Value with { Status = request.Status, StaffNote = request.StaffNote?.Trim(), UpdatedAt = DateTimeOffset.UtcNow };
                await applications.ReplaceOneAsync(s, x => x.Id == application.Id,
                    new Entry<ApplicationRecord> { Id = application.Id, Value = updated }, cancellationToken: ct);
                return updated;
            });
        }
        catch (MongoWriteException e) when (e.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            throw new JourneyConflict("duplicate_application", "Another active application already exists for this pet.");
        }
    }

    public async Task<AdoptionRecord> Finalise(AdoptionRequest request)
    {
        await EnsureIndexes();
        using var session = await db.Database.Client.StartSessionAsync();
        try
        {
            return await session.WithTransactionAsync(async (s, ct) =>
            {
                var application = (await applications.Find(s, x => x.Id == request.ApplicationId).FirstOrDefaultAsync(ct))?.Value;
                if (application?.Status != "Approved")
                    throw new JourneyConflict("approval_required", "Approve the application before finalising adoption.");
                var animal = (await animals.Find(s, x => x.Id == application.AnimalId).FirstOrDefaultAsync(ct))?.Value;
                if (animal == null || animal.Status == "Adopted" ||
                    await adoptions.Find(s, x => x.Value.AnimalId == application.AnimalId || x.Value.ApplicationId == application.Id).AnyAsync(ct))
                    throw new JourneyConflict("already_adopted", "This pet or application already has a finalised adoption.");

                var adoption = new AdoptionRecord(Guid.NewGuid().ToString("N"), application.Id, animal.Id,
                    application.AdopterId, DateTimeOffset.UtcNow, request.CollectionDate,
                    request.CareInstructions.Trim(), request.VaccinationNotes?.Trim(), request.FollowUpDueAt);
                await adoptions.InsertOneAsync(s, new Entry<AdoptionRecord> { Id = adoption.Id, Value = adoption }, cancellationToken: ct);
                await animals.UpdateOneAsync(s, x => x.Id == animal.Id,
                    Builders<Entry<Animal>>.Update.Set(x => x.Value.Status, "Adopted"), cancellationToken: ct);
                // Updating the application participates in conflict detection with simultaneous staff decisions.
                await applications.UpdateOneAsync(s, x => x.Id == application.Id,
                    Builders<Entry<ApplicationRecord>>.Update.Set(x => x.Value.UpdatedAt, DateTimeOffset.UtcNow), cancellationToken: ct);
                return adoption;
            });
        }
        catch (MongoWriteException e) when (e.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            throw new JourneyConflict("already_adopted", "This pet or application already has a finalised adoption.");
        }
    }
}
