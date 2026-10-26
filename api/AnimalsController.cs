using Microsoft.AspNetCore.Mvc;

using MongoDB.Driver;

namespace YeboPets.Api;

[Route("api/animals")]
public sealed class AnimalsController(Db db) : ApiController
{
    private static readonly string[] ValidStatuses = ["Draft", "Published", "Paused", "Adopted"];

    [HttpGet]
    public async Task<ActionResult<List<Animal>>> Browse([FromQuery] AnimalQuery query)
    {
        var animals = await db.All<Animal>("animals");
        return AnimalRules.Filter(animals, query);
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<Animal>> Details(string id)
    {
        var animal = await db.Get<Animal>("animals", id);

        if (animal is null || animal.Status != "Published")
            return NotFound(new ApiError("animal_unavailable", "This pet is unavailable."));

        return animal;
    }

    [HttpGet("/api/staff/animals")]
    public async Task<ActionResult<List<Animal>>> StaffAnimals()
    {
        if (!await RoleAccess.Is(db, Uid, "ShelterStaff"))
            return StatusCode(403, new ApiError("staff_access_required", "Shelter staff access is required."));

        return (await db.All<Animal>("animals")).OrderBy(a => a.Name).ToList();
    }

    [HttpPost]
    public async Task<ActionResult<Animal>> Create(Animal animal)
    {
        if (!await RoleAccess.Is(db, Uid, "ShelterStaff"))
            return StatusCode(403, new ApiError("staff_access_required", "Shelter staff access is required."));

        var error = Validate(animal);
        if (error is not null) return BadRequest(error);
        if (await db.Get<Animal>("animals", animal.Id) is not null)
            return Conflict(new ApiError("animal_exists", "An animal with this identifier already exists."));

        animal = animal with { PhotoUrl = null };
        await db.Put("animals", animal.Id, animal);
        return CreatedAtAction(nameof(Details), new { id = animal.Id }, animal);
    }

    [HttpPatch("{id}")]
    public async Task<ActionResult<Animal>> Update(string id, Animal animal)
    {
        if (!await RoleAccess.Is(db, Uid, "ShelterStaff"))
            return StatusCode(403, new ApiError("staff_access_required", "Shelter staff access is required."));

        var existing = await db.Get<Animal>("animals", id);
        if (existing is null)
            return NotFound(new ApiError("animal_missing", "This animal does not exist."));

        var updated = animal with { Id = id, PhotoUrl = existing.PhotoUrl };
        var error = Validate(updated);
        if (error is not null) return BadRequest(error);

        if (existing.Status == "Adopted" && updated.Status != "Adopted")
            return Conflict(new ApiError("already_adopted", "A finalised adoption cannot be reopened by editing the pet."));

        // Change profile fields only. A concurrent upload owns PhotoUrl, and a finalisation owns Adopted status.
        var change = Builders<Entry<Animal>>.Update
            .Set(x => x.Value.Name, updated.Name).Set(x => x.Value.Species, updated.Species)
            .Set(x => x.Value.Breed, updated.Breed).Set(x => x.Value.AgeMonths, updated.AgeMonths)
            .Set(x => x.Value.Size, updated.Size).Set(x => x.Value.Sex, updated.Sex)
            .Set(x => x.Value.Activity, updated.Activity).Set(x => x.Value.Area, updated.Area)
            .Set(x => x.Value.NeedsYard, updated.NeedsYard).Set(x => x.Value.GoodWithChildren, updated.GoodWithChildren)
            .Set(x => x.Value.GoodWithPets, updated.GoodWithPets).Set(x => x.Value.ExperienceRequired, updated.ExperienceRequired)
            .Set(x => x.Value.Care, updated.Care).Set(x => x.Value.Description, updated.Description)
            .Set(x => x.Value.Status, updated.Status);
        var saved = await db.Database.GetCollection<Entry<Animal>>("animals").FindOneAndUpdateAsync(
            x => x.Id == id && x.Value.Status == existing.Status, change,
            new FindOneAndUpdateOptions<Entry<Animal>> { ReturnDocument = ReturnDocument.After });
        if (saved == null) return Conflict(new ApiError("animal_changed", "This pet changed. Reload its profile before editing."));
        return saved.Value;
    }

    private static ApiError? Validate(Animal animal)
    {
        if (string.IsNullOrWhiteSpace(animal.Id) || string.IsNullOrWhiteSpace(animal.Name) ||
            string.IsNullOrWhiteSpace(animal.Breed) || !ValidStatuses.Contains(animal.Status) ||
            animal.AgeMonths is < 1 or > 300)
        {
            return new ApiError("validation_failed", "Complete the animal profile with valid values.");
        }

        return null;
    }
}
