using System.Net;
using System.Net.Http.Json;
using YeboPets.Api;
using Xunit;

namespace YeboPets.Api.Tests;

public sealed class AccountAccessTests(ApiFactory factory) : IClassFixture<ApiFactory>
{
    [Fact]
    public async Task OrdinaryAccountGetsOnlyAdopterAccessAndCannotClaimStaff()
    {
        using var client = factory.AsUser("test-access");
        var access = await client.GetFromJsonAsync<UserAccount>("/api/me/access?requestedRole=ShelterStaff&roles=ShelterStaff");
        Assert.Equal("test-access", access!.Id);
        Assert.Equal(new[] { "Adopter" }, access.Roles);
        Assert.Equal(HttpStatusCode.Forbidden, (await client.GetAsync("/api/me?requestedRole=ShelterStaff")).StatusCode);
        Assert.False(await RoleAccess.Is(factory.Database, "test-access", "ShelterStaff"));
    }

    [Fact]
    public async Task ApprovedStaffRetainsBothPermissionsWhenSwitchingWorkspaces()
    {
        using var client = factory.AsUser("staff-access");
        var access = await client.GetFromJsonAsync<UserAccount>("/api/me/access");
        Assert.Equal(new[] { "Adopter", "ShelterStaff" }, access!.Roles);
        foreach (var role in new[] { "ShelterStaff", "Adopter" })
        {
            var selected = await client.GetFromJsonAsync<UserAccount>("/api/me?requestedRole=" + role);
            Assert.Equal(role, selected!.Role);
            Assert.True(await RoleAccess.Is(factory.Database, "staff-access", "Adopter"));
            Assert.True(await RoleAccess.Is(factory.Database, "staff-access", "ShelterStaff"));
            Assert.Equal(HttpStatusCode.OK, (await client.GetAsync("/api/adopters/me/saved")).StatusCode);
        }
    }

    [Fact]
    public async Task AccessRefreshEnablesStaffSavedPetSyncBeforeChoosingAWorkspace()
    {
        using var client = factory.AsUser("staff-access");
        Assert.Equal(HttpStatusCode.Forbidden, (await client.GetAsync("/api/adopters/me/saved")).StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await client.GetAsync("/api/me/access")).StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await client.GetAsync("/api/adopters/me/saved")).StatusCode);
        Assert.True(await RoleAccess.Is(factory.Database, "staff-access", "ShelterStaff"));
    }

    [Fact]
    public async Task RefreshingAccessRemovesUnapprovedLegacyStaffRole()
    {
        using var client = factory.AsUser("staff-no-longer-approved");
        var access = await client.GetFromJsonAsync<UserAccount>("/api/me/access");
        Assert.Equal("Adopter", access!.Role);
        Assert.Equal(new[] { "Adopter" }, access.Roles);
        Assert.False(await RoleAccess.Is(factory.Database, access.Id, "ShelterStaff"));
    }
}
