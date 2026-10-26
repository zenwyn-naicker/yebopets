using System.ComponentModel.DataAnnotations;

namespace YeboPets.Api;

public record AdopterProfile : IValidatableObject
{
    [Required]
    [RegularExpression(@"^\+?[0-9]{9,15}$")]
    public string Phone { get; init; } = "";

    [Required]
    [RegularExpression("Durban North|Berea|Umhlanga|Westville")]
    public string Area { get; init; } = "Durban North";

    [Required]
    [RegularExpression("House|Apartment|Townhouse")]
    public string Housing { get; init; } = "House";

    public bool HasSecureYard { get; init; }
    public bool HasChildren { get; init; }

    [Range(0, 5)]
    public int ExistingPets { get; init; }

    [Range(0, 2)]
    public int Experience { get; init; }

    [Required]
    [RegularExpression("Calm|Moderate|Active")]
    public string Activity { get; init; } = "Moderate";

    [Required]
    [RegularExpression("Any|Dog|Cat")]
    public string PreferredSpecies { get; init; } = "Any";

    [Required]
    [RegularExpression("Any|Small|Medium|Large")]
    public string PreferredSize { get; init; } = "Any";

    [Required]
    [RegularExpression("Any|Young|Adult|Senior")]
    public string PreferredAge { get; init; } = "Any";

    [Range(5, 50)]
    public int MaxDistanceKm { get; init; } = 25;

    public bool CareReady { get; init; }

    public IEnumerable<ValidationResult> Validate(ValidationContext context)
    {
        if (!CareReady)
        {
            yield return new ValidationResult(
                "Confirm that you are ready for long-term care.",
                [nameof(CareReady)]);
        }
    }
}