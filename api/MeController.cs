using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;

namespace YeboPets.Api;

public record UserAccount(string Id, string Name, string Email, string Role, IReadOnlyList<string>? Roles = null);

[Route("api/me")]
public sealed class MeController(Db db, IConfiguration configuration) : ApiController
{
    // Available workspaces come from the verified identity and server allowlist, never a client role claim.
    [HttpGet("access")]
    public async Task<ActionResult<UserAccount>> Access()
    {
        var email = User.FindFirstValue(ClaimTypes.Email) ?? "";
        var roles = RolesFor(email);
        var previous = await db.Get<UserAccount>("users", Uid);
        var role = previous != null && roles.Contains(previous.Role) ? previous.Role : "Adopter";
        var account = new UserAccount(Uid, User.FindFirstValue(ClaimTypes.Name) ?? "", email, role, roles);
        await db.Put("users", Uid, account);
        return account;
    }

    private string[] RolesFor(string email) =>
        (configuration.GetSection("StaffEmails").Get<string[]>() ?? [])
            .Contains(email, StringComparer.OrdinalIgnoreCase)
                ? ["Adopter", "ShelterStaff"] : ["Adopter"];

    [HttpGet]
    public async Task<ActionResult<UserAccount>> Me([FromQuery] string requestedRole = "Adopter")
    {
        if (requestedRole is not ("Adopter" or "ShelterStaff"))
            return BadRequest(new ApiError("invalid_role", "Choose a valid account type."));

        var email = User.FindFirstValue(ClaimTypes.Email) ?? "";
        var roles = RolesFor(email);

        if (!roles.Contains(requestedRole))
        {
            return StatusCode(403, new ApiError(
                "staff_access_required",
                "This Google account is not approved for shelter access."));
        }

        var account = new UserAccount(
            Uid,
            User.FindFirstValue(ClaimTypes.Name) ?? "",
            email,
            requestedRole,
            roles
        );

        await db.Put("users", Uid, account);
        return account;
    }
}