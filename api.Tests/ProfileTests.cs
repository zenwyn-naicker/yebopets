using System.Net;
using System.Net.Http.Json;
using YeboPets.Api;
using Xunit;

namespace YeboPets.Api.Tests;

public sealed class ProfileTests(ApiFactory factory) : IClassFixture<ApiFactory>
{
	[Fact]
	public async Task InvalidPhoneAndReadinessAreRejected()
	{
		var response = await factory.AsUser("test-profile-invalid")
			.PutAsJsonAsync("/api/adopters/me/profile", new AdopterProfile
			{
				Phone = "abc",
				CareReady = false
			});

		Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
	}

	[Fact]
	public async Task ProfilesPersistAndRemainUserScoped()
	{
		var first = factory.AsUser("test-profile-a");
		var second = factory.AsUser("test-profile-b");

		var profile = new AdopterProfile
		{
			Phone = "+27821234567",
			CareReady = true,
			Area = "Durban North"
		};

		var write = await first.PutAsJsonAsync(
			"/api/adopters/me/profile", profile);

		Assert.Equal(HttpStatusCode.OK, write.StatusCode);

		var saved = await first.GetFromJsonAsync<AdopterProfile>(
			"/api/adopters/me/profile");

		Assert.Equal(profile.Phone, saved!.Phone);

		var other = await second.GetAsync("/api/adopters/me/profile");
		Assert.Equal(HttpStatusCode.NotFound, other.StatusCode);
	}
}