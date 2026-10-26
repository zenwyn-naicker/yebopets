/**************************************************************************************************************
 * Author: MongoDB, Inc.
 * Date: 2026
 * Title: Atomicity and transactions
 * Type: [Source Code]
 * Available at: <https://www.mongodb.com/docs/manual/core/write-operations-atomicity/> [Accessed 20 October 2026]
 **************************************************************************************************************/

using System.ComponentModel.DataAnnotations;
using System.Security.Cryptography;
using System.Text;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;

namespace YeboPets.Api;

public record SavedOperation(
    [Required] string ClientOperationId,
    [Required, StringLength(100, MinimumLength = 1)] string AnimalId,
    bool Saved);
public record SyncReply(string ClientOperationId, bool Replayed);

[Route("api/sync")]
public sealed class SyncController(Db db) : ApiController
{
    [HttpPost]
    public async Task<ActionResult<SyncReply>> Apply(SavedOperation operation)
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));
        if (!Guid.TryParseExact(operation.ClientOperationId, "N", out _))
            return BadRequest(new ApiError("invalid_operation", "Use a UUID without hyphens."));
        var records = db.Database.GetCollection<SavedRecord>("saved");
        var key = operation.ClientOperationId;
        var hash = Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(
            operation.AnimalId + "\n" + operation.Saved)));

        var existing = await records.Find(x => x.Id == Uid).FirstOrDefaultAsync();
        if (existing?.Operations.TryGetValue(key, out var previous) == true)
            return previous == hash ? Ok(new SyncReply(key, true))
                : Conflict(new ApiError("operation_reused", "This operation ID has a different payload."));
        if (operation.Saved && (await db.Get<Animal>("animals", operation.AnimalId))?.Status != "Published")
            return NotFound(new ApiError("animal_unavailable", "This pet is unavailable."));

        try
        {
            await records.UpdateOneAsync(x => x.Id == Uid,
                Builders<SavedRecord>.Update.SetOnInsert(x => x.AnimalIds, new List<string>()),
                new UpdateOptions { IsUpsert = true });
        }
        catch (MongoWriteException e) when (e.WriteError.Category == ServerErrorCategory.DuplicateKey) { }
        var filter = Builders<SavedRecord>.Filter.Eq(x => x.Id, Uid) &
            Builders<SavedRecord>.Filter.Exists("Operations." + key, false);
        var change = operation.Saved
            ? Builders<SavedRecord>.Update.AddToSet(x => x.AnimalIds, operation.AnimalId)
            : Builders<SavedRecord>.Update.Pull(x => x.AnimalIds, operation.AnimalId);
        var result = await records.UpdateOneAsync(filter,
            change.Set("Operations." + key, hash));
        if (result.ModifiedCount == 0)
        {
            var winner = await records.Find(x => x.Id == Uid).FirstAsync();
            if (winner.Operations[key] != hash)
                return Conflict(new ApiError("operation_reused", "This operation ID has a different payload."));
        }
        return new SyncReply(key, result.ModifiedCount == 0);
    }
}