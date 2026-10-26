using System.ComponentModel.DataAnnotations;

namespace YeboPets.Api;

public record Animal(
    string Id,
    string Name,
    string Species,
    string Breed,
    int AgeMonths,
    string Size,
    string Sex,
    string Activity,
    string Area,
    bool NeedsYard,
    bool GoodWithChildren,
    bool GoodWithPets,
    int ExperienceRequired,
    string Care,
    string Description,
    string Status = "Published",
    double? DistanceKm = null,
    string? PhotoUrl = null
);

public record AnimalQuery
{
    [StringLength(80)]
    public string? Q { get; init; }

    [Required]
    [RegularExpression("Any|Dog|Cat")]
    public string Species { get; init; } = "Any";

    [Required]
    [RegularExpression("Any|Small|Medium|Large")]
    public string Size { get; init; } = "Any";

    [Required]
    [RegularExpression("Any|Calm|Moderate|Active|Very active")]
    public string Activity { get; init; } = "Any";

    [Required]
    [RegularExpression("Any|Female|Male")]
    public string Sex { get; init; } = "Any";

    [Required]
    [RegularExpression("Any|Young|Adult|Senior")]
    public string AgeGroup { get; init; } = "Any";

    [Required]
    [RegularExpression("Any|Durban North|Berea|Umhlanga|Westville")]
    public string Area { get; init; } = "Any";

    [Range(5, 50)]
    public int RadiusKm { get; init; } = 25;
}

public static class Geography
{
    private static readonly Dictionary<string, (double Lat, double Lon)> Towns = new()
    {
        ["Durban North"] = (-29.7853, 31.0413),
        ["Berea"] = (-29.8495, 30.9934),
        ["Umhlanga"] = (-29.7260, 31.0850),
        ["Westville"] = (-29.8277, 30.9280)
    };

    public static double Distance(string first, string second)
    {
        var a = Towns[first];
        var b = Towns[second];

        static double Radians(double degrees) => degrees * Math.PI / 180;

        var latitude = Radians(b.Lat - a.Lat);
        var longitude = Radians(b.Lon - a.Lon);

        var value = Math.Pow(Math.Sin(latitude / 2), 2)
            + Math.Cos(Radians(a.Lat)) * Math.Cos(Radians(b.Lat))
            * Math.Pow(Math.Sin(longitude / 2), 2);

        return 6371 * 2 * Math.Asin(Math.Sqrt(Math.Clamp(value, 0, 1)));
    }
}

public static class AnimalRules
{
    public static string AgeGroup(int months) =>
        months < 12 ? "Young" : months < 96 ? "Adult" : "Senior";

    public static List<Animal> Filter(IEnumerable<Animal> animals, AnimalQuery query)
    {
        var search = (query.Q ?? "").Trim();

        return animals
            .Where(a => a.Status == "Published")
            .Where(a => search.Length == 0
                || a.Name.Contains(search, StringComparison.OrdinalIgnoreCase)
                || a.Breed.Contains(search, StringComparison.OrdinalIgnoreCase))
            .Where(a => query.Species == "Any" || a.Species == query.Species)
            .Where(a => query.Size == "Any" || a.Size == query.Size)
            .Where(a => query.Activity == "Any" || a.Activity == query.Activity)
            .Where(a => query.Sex == "Any" || a.Sex == query.Sex)
            .Where(a => query.AgeGroup == "Any" || AgeGroup(a.AgeMonths) == query.AgeGroup)
            .Select(a => a with
            {
                DistanceKm = query.Area == "Any"
                    ? null
                    : Math.Round(Geography.Distance(query.Area, a.Area), 1)
            })
            .Where(a => a.DistanceKm is null || a.DistanceKm <= query.RadiusKm)
            .OrderBy(a => a.Name)
            .ToList();
    }
}