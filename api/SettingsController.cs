using Microsoft.AspNetCore.Mvc;

namespace YeboPets.Api;

[Route("api/me/settings")]
public sealed class SettingsController(Db db) : ApiController
{
    [HttpGet]
    public async Task<ActionResult<UserSettings>> Get() =>
        await db.Get<UserSettings>("settings", Uid) ?? new UserSettings();

    [HttpPut]
    public async Task<ActionResult<UserSettings>> Put(UserSettings settings)
    {
        await db.Put("settings", Uid, settings);
        return settings;
    }
}