using Microsoft.AspNetCore.Mvc;

namespace YeboPets.Api;

[Route("api/adopters/me/profile")]
public sealed class ProfileController(Db db) : ApiController
{
    [HttpGet]
    public async Task<ActionResult<AdopterProfile>> Get()
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        var profile = await db.Get<AdopterProfile>("profiles", Uid);

        if (profile is null)
            return NotFound(new ApiError("profile_missing", "Complete your adopter profile."));

        return profile;
    }

    [HttpPut]
    public async Task<ActionResult<AdopterProfile>> Put(AdopterProfile profile)
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        await db.Put("profiles", Uid, profile);
        return profile;
    }
}