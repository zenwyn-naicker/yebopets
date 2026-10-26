using System.Net;
using System.Net.Http.Json;
using YeboPets.Api;
using Xunit;

namespace YeboPets.Api.Tests;

public sealed class SavedTests(ApiFactory factory) : IClassFixture<ApiFactory>
{
	[Fact]
	public async Task SavingIsIdempotentAndUserScoped()
	{
		await factory.Database.Put(
			"animals", "saved-test", AnimalTests.Sample("saved-test"));

		var first = factory.AsUser("test-saved-a");
		var second = factory.AsUser("test-saved-b");

		Assert.Equal(
			HttpStatusCode.NoContent,
			(await first.PutAsync(
				"/api/adopters/me/saved/saved-test", null)).StatusCode);

		await first.PutAsync("/api/adopters/me/saved/saved-test", null);

		var own = await first.GetFromJsonAsync<List<Animal>>("/api/adopters/me/saved");
		var other = await second.GetFromJsonAsync<List<Animal>>("/api/adopters/me/saved");

		Assert.Single(own!);
		Assert.Empty(other!);

		await first.DeleteAsync("/api/adopters/me/saved/saved-test");
		await first.DeleteAsync("/api/adopters/me/saved/saved-test");

		var remaining = await first.GetFromJsonAsync<List<Animal>>(
			"/api/adopters/me/saved");

		Assert.Empty(remaining!);
	}

	[Fact]
	public async Task MissingAnimalsCannotBeSaved()
	{
		var response = await factory.AsUser("test-saved-missing")
			.PutAsync("/api/adopters/me/saved/does-not-exist", null);

		Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
	}
}