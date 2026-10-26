using Microsoft.AspNetCore.Mvc;

namespace YeboPets.Api;

[Route("api/animals/{id}/match")]
public sealed class MatchController(Db db) : ApiController
{
    [HttpGet]
    public async Task<ActionResult<MatchResult>> Get(string id)
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        var animal = await db.Get<Animal>("animals", id);

        if (animal is null || animal.Status != "Published")
            return NotFound(new ApiError("animal_unavailable", "This pet is unavailable."));

        var profile = await db.Get<AdopterProfile>("profiles", Uid);

        if (profile is null || !profile.CareReady)
        {
            return Conflict(new ApiError(
                "profile_required",
                "Complete and save your adopter profile first."));
        }

        var language=(await db.Get<UserSettings>("settings",Uid))?.Language ?? "en";
        return MatchLanguage.Apply(Matching.Calculate(profile, animal),profile,animal,language);
    }
}