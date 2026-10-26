/**************************************************************************************************************
 * Author: MongoDB, Inc.
 * Date: 2026
 * Title: MongoDB .NET/C# Driver
 * Type: [Source Code]
 * Available at: <https://www.mongodb.com/docs/drivers/csharp/current/> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: MongoDB, Inc.
 * Date: 2026
 * Title: Update One
 * Type: [Source Code]
 * Available at: <https://www.mongodb.com/docs/drivers/csharp/current/crud/update-one/> [Accessed 9 September 2026]
 **************************************************************************************************************/

using MongoDB.Bson.Serialization.Attributes;
using MongoDB.Driver;

namespace YeboPets.Api;

public sealed class Entry<T>
{
    [BsonId]
    public string Id { get; set; } = "";

    public T Value { get; set; } = default!;
}

public sealed class Db
{
    public IMongoDatabase Database { get; }

    public Db(IConfiguration configuration)
    {
        var connection = configuration["Mongo:ConnectionString"]
            ?? throw new InvalidOperationException("MongoDB configuration is missing.");

        var settings = MongoClientSettings.FromConnectionString(connection);
        settings.ServerSelectionTimeout = TimeSpan.FromSeconds(10);

        Database = new MongoClient(settings).GetDatabase(
            configuration["Mongo:Database"] ?? "yebopets");
    }

    private IMongoCollection<Entry<T>> Collection<T>(string name) =>
        Database.GetCollection<Entry<T>>(name);

    public async Task<T?> Get<T>(string collection, string id)
    {
        var record = await Collection<T>(collection)
            .Find(item => item.Id == id)
            .FirstOrDefaultAsync();

        return record is null ? default : record.Value;
    }

    public async Task<List<T>> All<T>(string collection)
    {
        var records = await Collection<T>(collection)
            .Find(FilterDefinition<Entry<T>>.Empty)
            .ToListAsync();

        return records.Select(item => item.Value).ToList();
    }

    public async Task Put<T>(string collection, string id, T value)
    {
        var records = Collection<T>(collection);
        var replacement = new Entry<T> { Id = id, Value = value };

        try
        {
            await records.ReplaceOneAsync(
                item => item.Id == id,
                replacement,
                new ReplaceOptions { IsUpsert = true });
        }
        catch (MongoWriteException error)
            when (error.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            await records.ReplaceOneAsync(item => item.Id == id, replacement);
        }
    }

    public Task InsertIfMissing<T>(string collection, string id, T value) =>
        Collection<T>(collection).UpdateOneAsync(
            item => item.Id == id,
            Builders<Entry<T>>.Update.SetOnInsert(item => item.Value, value),
            new UpdateOptions { IsUpsert = true });
}
