using System.Net;
using System.Net.Http.Json;
using YeboPets.Api;
using Xunit;

namespace YeboPets.Api.Tests;

public sealed class SettingsTests(ApiFactory factory) : IClassFixture<ApiFactory>
{
    [Fact]
    public async Task SettingsPersistAndRemainUserScoped()
    {
        var first = factory.AsUser("test-settings-a");
        var second = factory.AsUser("test-settings-b");

        var settings = new UserSettings
        {
            SearchRadiusKm = 40,
            PreferredSpecies = "Cat"
        };

        var response = await first.PutAsJsonAsync("/api/me/settings", settings);
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);

        var own = await first.GetFromJsonAsync<UserSettings>("/api/me/settings");
        var other = await second.GetFromJsonAsync<UserSettings>("/api/me/settings");

        Assert.Equal(40, own!.SearchRadiusKm);
        Assert.Equal(25, other!.SearchRadiusKm);
    }

    [Fact]
    public async Task InvalidSettingsAreRejected()
    {
        var response = await factory.AsUser("test-settings-invalid")
            .PutAsJsonAsync("/api/me/settings", new
            {
                searchRadiusKm = -1,
                preferredSpecies = "Dragon"
            });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task IdentityComesFromTheVerifiedToken()
    {
        var account = await factory.AsUser("test-account")
            .GetFromJsonAsync<UserAccount>("/api/me?requestedRole=Adopter");

        Assert.Equal("test-account", account!.Id);
        Assert.Equal("Adopter", account.Role);
    }
}