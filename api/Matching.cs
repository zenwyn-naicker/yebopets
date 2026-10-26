namespace YeboPets.Api;

public record MatchFactor(string Name, int Points, int Maximum, string Reason);

public record MatchResult(
    string AnimalId,
    int OverallScore,
    double DistanceKm,
    List<MatchFactor> Factors,
    string Advice
);

public static class Matching
{
    public static MatchResult Calculate(AdopterProfile profile, Animal animal)
    {
        var householdIssues = new List<string>();

        if (animal.NeedsYard && !profile.HasSecureYard)
            householdIssues.Add("A secure yard is required.");

        if (profile.HasChildren && !animal.GoodWithChildren)
            householdIssues.Add("This pet is not listed as suitable with children.");

        if (profile.ExistingPets > 0 && !animal.GoodWithPets)
            householdIssues.Add("This pet is not listed as suitable with other pets.");

        var household = householdIssues.Count == 0 ? 30 : 0;

        var levels = new[] { "Calm", "Moderate", "Active", "Very active" };

        var difference = Math.Abs(
            Array.IndexOf(levels, profile.Activity) -
            Array.IndexOf(levels, animal.Activity));

        var lifestyle = difference switch
        {
            0 => 25,
            1 => 12,
            _ => 0
        };

        var speciesMatch =
            profile.PreferredSpecies == "Any" ||
            profile.PreferredSpecies == animal.Species;

        var sizeMatch =
            profile.PreferredSize == "Any" ||
            profile.PreferredSize == animal.Size;

        var ageMatch =
            profile.PreferredAge == "Any" ||
            profile.PreferredAge == AnimalRules.AgeGroup(animal.AgeMonths);

        var preferences =
            (speciesMatch ? 8 : 0) +
            (sizeMatch ? 6 : 0) +
            (ageMatch ? 6 : 0);

        var experience =
            profile.Experience >= animal.ExperienceRequired ? 15 : 0;

        var distanceKm = Geography.Distance(profile.Area, animal.Area);

        var distance = (int)Math.Round(
            10 * Math.Clamp(1 - distanceKm / profile.MaxDistanceKm, 0, 1),
            MidpointRounding.AwayFromZero);

        var factors = new List<MatchFactor>
        {
            new("Household", household, 30,
                householdIssues.Count == 0
                    ? "The listed household needs match your answers."
                    : string.Join(" ", householdIssues)),

            new("Lifestyle", lifestyle, 25,
                $"Your activity is {profile.Activity}; this pet's activity is {animal.Activity}."),

            new("Preferences", preferences, 20,
                $"Species match: {speciesMatch}; size match: {sizeMatch}; age match: {ageMatch}."),

            new("Experience", experience, 15,
                $"Your experience is {profile.Experience}/2; required level is {animal.ExperienceRequired}/2."),

            new("Distance", distance, 10,
                $"Approximately {distanceKm:F1} km between town centres; your limit is {profile.MaxDistanceKm} km.")
        };

        return new MatchResult(
            animal.Id,
            factors.Sum(factor => factor.Points),
            Math.Round(distanceKm, 1),
            factors,
            "This is an advisory score, not adoption approval. Household concerns require shelter review."
        );
    }
}