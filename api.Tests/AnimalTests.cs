using System.Net;
using YeboPets.Api;
using Xunit;

namespace YeboPets.Api.Tests;

public sealed class AnimalTests(ApiFactory factory) : IClassFixture<ApiFactory>
{
    public static Animal Sample(string id = "test-pet") => new(
        id, "Luna", "Dog", "Labrador cross", 24, "Large", "Female",
        "Active", "Durban North", true, true, true, 1,
        "Daily walks", "Sample pet");

    [Fact]
    public void SearchTrimsWhitespaceAndIgnoresCase()
    {
        var result = AnimalRules.Filter(
            [Sample()],
            new AnimalQuery { Q = "  LABRADOR  " });

        Assert.Single(result);
    }

    [Fact]
    public void UnpublishedAnimalsAreHidden()
    {
        var result = AnimalRules.Filter(
            [Sample() with { Status = "Paused" }],
            new AnimalQuery());

        Assert.Empty(result);
    }

    [Fact]
    public void DistanceFilterExcludesDistantAnimals()
    {
        var result = AnimalRules.Filter(
            [Sample() with { Area = "Westville" }],
            new AnimalQuery { Area = "Umhlanga", RadiusKm = 5 });

        Assert.Empty(result);
    }

    [Fact]
    public async Task EmptySearchIsAccepted()
    {
        var response = await factory.AsUser("test-empty-search")
            .GetAsync("/api/animals?q=");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task MissingAndInvalidTokensAreRejected()
    {
        Assert.Equal(
            HttpStatusCode.Unauthorized,
            (await factory.CreateClient().GetAsync("/api/animals")).StatusCode);

        Assert.Equal(
            HttpStatusCode.Unauthorized,
            (await factory.AsUser("invalid").GetAsync("/api/animals")).StatusCode);
    }

    [Fact]
    public async Task DetailsUseTheSelectedIdentifier()
    {
        await factory.Database.Put("animals", "chosen", Sample("chosen"));

        var response = await factory.AsUser("test-rivaan")
            .GetAsync("/api/animals/chosen");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Contains("\"id\":\"chosen\"", await response.Content.ReadAsStringAsync());
    }
}