/**************************************************************************************************************
 * Author: Microsoft
 * Date: 2026
 * Title: Integration tests in ASP.NET Core
 * Type: [Source Code]
 * Available at: <https://learn.microsoft.com/aspnet/core/test/integration-tests> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Mongo2Go
 * Date: 2026
 * Title: Mongo2Go
 * Type: [Source Code]
 * Available at: <https://github.com/Mongo2Go/Mongo2Go> [Accessed 9 September 2026]
 **************************************************************************************************************/

using System.Net.Http.Headers;
using System.Security.Claims;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Mongo2Go;
using YeboPets.Api;

namespace YeboPets.Api.Tests;

public sealed class ApiFactory : WebApplicationFactory<Program>
{
    private readonly MongoDbRunner mongo = MongoDbRunner.Start(singleNodeReplSet: true);

    public string DatabaseName { get; } =
        "yebopets_test_" + Guid.NewGuid().ToString("N");

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureAppConfiguration((_, config) =>
            config.AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Mongo:ConnectionString"] = mongo.ConnectionString,
                ["Mongo:Database"] = DatabaseName,
                ["StaffEmails:0"] = "staff-access@example.com"
            }));

        builder.ConfigureServices(services =>
        {
            services.RemoveAll<ITokens>();
            services.AddSingleton<ITokens, TestTokens>();
        });
    }

    public HttpClient AsUser(string uid)
    {
        var role = uid.StartsWith("staff-") ? "ShelterStaff" : "Adopter";
        Database.Put("users", uid, new UserAccount(uid, "Test User", $"{uid}@example.com", role)).GetAwaiter().GetResult();
        var client = CreateClient();
        client.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", uid);

        return client;
    }

    public Db Database => Services.GetRequiredService<Db>();

    protected override void Dispose(bool disposing)
    {
        base.Dispose(disposing);

        if (disposing)
            mongo.Dispose();
    }

    private sealed class TestTokens : ITokens
    {
        public Task<ClaimsPrincipal> Verify(string token)
        {
            if (!token.StartsWith("test-") && !token.StartsWith("staff-"))
                throw new UnauthorizedAccessException();

            var identity = new ClaimsIdentity(
            [
                new Claim(ClaimTypes.NameIdentifier, token),
                new Claim(ClaimTypes.Name, "Test User"),
                new Claim(ClaimTypes.Email, $"{token}@example.com")
            ], "Test");

            return Task.FromResult(new ClaimsPrincipal(identity));
        }
    }
}
