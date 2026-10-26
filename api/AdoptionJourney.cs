namespace YeboPets.Api;

public record ApplicationRecord(
    string Id,
    string AnimalId,
    string AdopterId,
    string Status,
    string Motivation,
    bool HousingPermissionConfirmed,
    bool LongTermCareConfirmed,
    string? AdditionalNotes,
    string? StaffNote,
    DateTimeOffset SubmittedAt,
    DateTimeOffset UpdatedAt
);

public record ApplicationRequest(
    string Motivation,
    bool HousingPermissionConfirmed,
    bool LongTermCareConfirmed,
    string? AdditionalNotes
);

public record ApplicationDecisionRequest(string Status, string? StaffNote);

public record ApplicationDetails(
    ApplicationRecord Application,
    List<AppointmentRecord> Appointments,
    AdoptionRecord? Adoption,
    List<FollowUpRecord> FollowUps);

public record AppointmentRecord(
    string Id,
    string ApplicationId,
    string Type,
    DateTimeOffset ScheduledAt,
    string LocationName,
    string Address,
    string? Notes,
    string Status,
    DateTimeOffset? UpdatedAt = null
);

public record AppointmentRequest(
    string Type,
    DateTimeOffset ScheduledAt,
    string LocationName,
    string Address,
    string? Notes
);

public record AppointmentUpdateRequest(string Status, DateTimeOffset? ScheduledAt,
    string? LocationName = null, string? Address = null, string? Notes = null);

public record AdoptionRecord(
    string Id,
    string ApplicationId,
    string AnimalId,
    string AdopterId,
    DateTimeOffset FinalisedAt,
    DateTimeOffset CollectionDate,
    string CareInstructions,
    string? VaccinationNotes,
    DateTimeOffset FollowUpDueAt
);

public record AdoptionRequest(
    string ApplicationId,
    DateTimeOffset CollectionDate,
    string CareInstructions,
    string? VaccinationNotes,
    DateTimeOffset FollowUpDueAt
);

public record FollowUpRecord(
    string Id,
    string AdoptionId,
    string AdopterId,
    string WellbeingStatus,
    string Notes,
    DateTimeOffset SubmittedAt,
    string? PhotoUrl = null
);

public record FollowUpRequest(string WellbeingStatus, string Notes, string? PhotoUrl = null);
