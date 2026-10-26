using System.ComponentModel.DataAnnotations;

namespace YeboPets.Api;

public record UserSettings
{
    [Required, RegularExpression("en|zu")]
    public string Language { get; init; } = "en";
    public bool NotificationsEnabled { get; init; } = true;
    public bool StatusAlertsEnabled { get; init; } = true;
    public bool AppointmentRemindersEnabled { get; init; } = true;
    public bool SyncOnMobileData { get; init; } = false;
    public bool BiometricsEnabled { get; init; } = false;
    [Range(5, 50)]
    public int SearchRadiusKm { get; init; } = 25;

    [Required]
    [RegularExpression("Any|Dog|Cat")]
    public string PreferredSpecies { get; init; } = "Any";
}