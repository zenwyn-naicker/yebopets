using System.Net;
using YeboPets.Api;
using Xunit;

namespace YeboPets.Api.Tests;

public sealed class MatchingTests(ApiFactory factory) : IClassFixture<ApiFactory>
{
    private static AdopterProfile SuitableProfile() => new()
    {
        Phone = "+27821234567",
        Area = "Durban North",
        HasSecureYard = true,
        Experience = 2,
        Activity = "Active",
        CareReady = true
    };

    [Fact]
    public void FullySuitableProfileScoresOneHundred()
    {
        var result = Matching.Calculate(
            SuitableProfile(), AnimalTests.Sample());

        Assert.Equal(100, result.OverallScore);
        Assert.Equal(100, result.Factors.Sum(f => f.Maximum));
        Assert.Equal(result.OverallScore, result.Factors.Sum(f => f.Points));
    }

    [Fact]
    public void MissingRequiredYardRemovesHouseholdPoints()
    {
        var result = Matching.Calculate(
            SuitableProfile() with { HasSecureYard = false },
            AnimalTests.Sample());

        Assert.Equal(70, result.OverallScore);
        Assert.Contains("secure yard", result.Factors[0].Reason);
    }

    [Fact]
    public void ScoreRemainsWithinBounds()
    {
        var profile = SuitableProfile() with
        {
            Area = "Westville",
            Activity = "Calm",
            HasSecureYard = false,
            Experience = 0,
            PreferredSpecies = "Cat",
            PreferredSize = "Small",
            PreferredAge = "Senior"
        };

        var result = Matching.Calculate(profile, AnimalTests.Sample());

        Assert.InRange(result.OverallScore, 0, 100);
        Assert.Equal(5, result.Factors.Count);
    }

    [Fact]
    public async Task MissingProfilePreventsMatching()
    {
        await factory.Database.Put(
            "animals", "match-test", AnimalTests.Sample("match-test"));

        var response = await factory.AsUser("test-no-profile")
            .GetAsync("/api/animals/match-test/match");

        Assert.Equal(HttpStatusCode.Conflict, response.StatusCode);
    }
}