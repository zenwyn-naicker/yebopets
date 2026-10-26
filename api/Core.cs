/**************************************************************************************************************
 * Author: Firebase
 * Date: 2026
 * Title: Verify ID Tokens
 * Type: [Source Code]
 * Available at: <https://firebase.google.com/docs/auth/admin/verify-id-tokens> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Google Cloud
 * Date: 2026
 * Title: Class CredentialFactory (1.73.0)
 * Type: [Source Code]
 * Available at: <https://docs.cloud.google.com/dotnet/docs/reference/Google.Apis/latest/Google.Apis.Auth.OAuth2.CredentialFactory> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Microsoft
 * Date: 2026
 * Title: Role-based authorization in ASP.NET Core
 * Type: [Source Code]
 * Available at: <https://learn.microsoft.com/aspnet/core/security/authorization/roles> [Accessed 9 September 2026]
 **************************************************************************************************************/

using System.Security.Claims;
using FirebaseAdmin;
using FirebaseAdmin.Auth;
using Google.Apis.Auth.OAuth2;
using Microsoft.AspNetCore.Mvc;

namespace YeboPets.Api;

public record ApiError(
    string Code,
    string Message,
    Dictionary<string, string[]>? FieldErrors = null
);

public interface ITokens
{
    Task<ClaimsPrincipal> Verify(string token);
}

public sealed class FirebaseTokens(IConfiguration configuration) : ITokens
{
    private readonly Lazy<FirebaseAuth> auth = new(() =>
    {
        var json = configuration["Firebase:ServiceAccountJson"]
            ?? throw new InvalidOperationException("Firebase configuration is missing.");

        var app = FirebaseApp.Create(new AppOptions
        {
            Credential = CredentialFactory
                .FromJson<ServiceAccountCredential>(json)
                .ToGoogleCredential(),
            ProjectId = configuration["Firebase:ProjectId"]
        });

        return FirebaseAuth.GetAuth(app);
    });

    public async Task<ClaimsPrincipal> Verify(string token)
    {
        try
        {
            var decoded = await auth.Value.VerifyIdTokenAsync(token, true);

            string ReadClaim(string name) =>
                decoded.Claims.TryGetValue(name, out var value)
                    ? value?.ToString() ?? ""
                    : "";

            var identity = new ClaimsIdentity(
            [
                new Claim(ClaimTypes.NameIdentifier, decoded.Uid),
                new Claim(ClaimTypes.Name, ReadClaim("name")),
                new Claim(ClaimTypes.Email, ReadClaim("email"))
            ], "Firebase");

            return new ClaimsPrincipal(identity);
        }
        catch (FirebaseAuthException)
        {
            throw new UnauthorizedAccessException();
        }
        catch (ArgumentException)
        {
            throw new UnauthorizedAccessException();
        }
    }
}

[ApiController]
public abstract class ApiController : ControllerBase
{
    protected string Uid =>
        User.FindFirstValue(ClaimTypes.NameIdentifier)
        ?? throw new UnauthorizedAccessException();
}

public static class RoleAccess
{
    public static async Task<bool> Is(Db db, string uid, string role)
    {
        var account = await db.Get<UserAccount>("users", uid);
        // Legacy records have one role; newer records retain every server-authorised workspace.
        return account?.Roles != null ? account.Roles.Contains(role) : account?.Role == role;
    }
}
