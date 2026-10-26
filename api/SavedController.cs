using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;

namespace YeboPets.Api;

[Route("api/adopters/me/saved")]
public sealed class SavedController(Db db) : ApiController
{
    private IMongoCollection<SavedRecord> Records =>
        db.Database.GetCollection<SavedRecord>("saved");

    [HttpGet]
    public async Task<ActionResult<List<Animal>>> Get()
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        var record = await Records.Find(item => item.Id == Uid)
            .FirstOrDefaultAsync();

        var ids = record?.AnimalIds.ToHashSet() ?? [];
        var animals = await db.All<Animal>("animals");

        return animals
            .Where(a => ids.Contains(a.Id) && a.Status == "Published")
            .OrderBy(a => a.Name)
            .ToList();
    }

    [HttpPut("{animalId}")]
    public async Task<IActionResult> Save(string animalId)
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        var animal = await db.Get<Animal>("animals", animalId);

        if (animal is null || animal.Status != "Published")
            return NotFound(new ApiError("animal_unavailable", "This pet is unavailable."));

        var update = Builders<SavedRecord>.Update
            .AddToSet(item => item.AnimalIds, animalId);

        try
        {
            await Records.UpdateOneAsync(
                item => item.Id == Uid,
                update,
                new UpdateOptions { IsUpsert = true });
        }
        catch (MongoWriteException error)
            when (error.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            await Records.UpdateOneAsync(item => item.Id == Uid, update);
        }

        return NoContent();
    }

    [HttpDelete("{animalId}")]
    public async Task<IActionResult> Remove(string animalId)
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        await Records.UpdateOneAsync(
            item => item.Id == Uid,
            Builders<SavedRecord>.Update.Pull(item => item.AnimalIds, animalId));

        return NoContent();
    }
}