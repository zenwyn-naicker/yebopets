using Microsoft.AspNetCore.Mvc;

namespace YeboPets.Api;

[Route("api")]
public sealed class AdoptionJourneyController(Db db, JourneyStore journey) : ApiController
{
    [HttpPost("animals/{animalId}/applications")]
    public async Task<ActionResult<ApplicationRecord>> Submit(string animalId, ApplicationRequest request)
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        if (request.Motivation.Trim().Length is < 20 or > 1000 ||
            !request.HousingPermissionConfirmed || !request.LongTermCareConfirmed)
        {
            return BadRequest(new ApiError("validation_failed", "Complete the motivation and required confirmations."));
        }

        var profile = await db.Get<AdopterProfile>("profiles", Uid);
        if (profile is null || !profile.CareReady)
            return Conflict(new ApiError("profile_required", "Complete and save your adopter profile first."));

        var animal = await db.Get<Animal>("animals", animalId);
        if (animal is null || animal.Status != "Published")
            return NotFound(new ApiError("animal_unavailable", "This pet is unavailable."));

        var now = DateTimeOffset.UtcNow;
        var application = new ApplicationRecord(
            Guid.NewGuid().ToString("N"), animalId, Uid, "Submitted",
            request.Motivation.Trim(), request.HousingPermissionConfirmed,
            request.LongTermCareConfirmed, request.AdditionalNotes?.Trim(), null, now, now);

        try { await journey.Submit(application); }
        catch (JourneyConflict conflict) { return Conflict(conflict.Error); }
        return Created($"/api/applications/{application.Id}", application);
    }

    [HttpGet("applications/me")]
    public async Task<ActionResult<List<ApplicationRecord>>> Mine()
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        return (await db.All<ApplicationRecord>("applications"))
            .Where(a => a.AdopterId == Uid).OrderByDescending(a => a.UpdatedAt).ToList();
    }

    [HttpGet("staff/applications")]
    public async Task<ActionResult<List<ApplicationRecord>>> StaffApplications()
    {
        if (!await RoleAccess.Is(db, Uid, "ShelterStaff"))
            return StatusCode(403, new ApiError("staff_access_required", "Shelter staff access is required."));

        return (await db.All<ApplicationRecord>("applications"))
            .OrderByDescending(a => a.UpdatedAt).ToList();
    }

    [HttpGet("applications/{id}")]
    public async Task<ActionResult<ApplicationDetails>> Details(string id)
    {
        var application = await db.Get<ApplicationRecord>("applications", id);
        if (application == null || !await CanRead(application.AdopterId)) return NotFound();
        var appointments = (await db.All<AppointmentRecord>("appointments"))
            .Where(x => x.ApplicationId == id).OrderBy(x => x.ScheduledAt).ToList();
        var adoption = (await db.All<AdoptionRecord>("adoptions")).FirstOrDefault(x => x.ApplicationId == id);
        var followUps = adoption == null ? new List<FollowUpRecord>() : await ReadFollowUps(adoption.Id);
        return new ApplicationDetails(application, appointments, adoption, followUps);
    }

    [HttpGet("adoptions/{id}/follow-ups")]
    public async Task<ActionResult<List<FollowUpRecord>>> FollowUps(string id)
    {
        var adoption = await db.Get<AdoptionRecord>("adoptions", id);
        if (adoption == null || !await CanRead(adoption.AdopterId)) return NotFound();
        return await ReadFollowUps(id);
    }

    private async Task<bool> CanRead(string adopterId) =>
        (adopterId == Uid && await RoleAccess.Is(db, Uid, "Adopter")) ||
        await RoleAccess.Is(db, Uid, "ShelterStaff");

    private async Task<List<FollowUpRecord>> ReadFollowUps(string adoptionId) =>
        (await db.All<FollowUpRecord>("followups")).Where(x => x.AdoptionId == adoptionId)
            .OrderByDescending(x => x.SubmittedAt).ToList();

    [HttpPatch("applications/{id}/decision")]
    public async Task<ActionResult<ApplicationRecord>> Decide(string id, ApplicationDecisionRequest request)
    {
        if (!await RoleAccess.Is(db, Uid, "ShelterStaff"))
            return StatusCode(403, new ApiError("staff_access_required", "Shelter staff access is required."));

        if (request.Status is not ("UnderReview" or "MoreInfo" or "Approved" or "Declined"))
            return BadRequest(new ApiError("invalid_status", "Choose a valid application decision."));

        var application = await db.Get<ApplicationRecord>("applications", id);
        if (application is null)
            return NotFound(new ApiError("application_missing", "This application does not exist."));

        try { return await journey.Decide(application, request); }
        catch (JourneyConflict conflict) { return Conflict(conflict.Error); }
    }

    [HttpPost("applications/{id}/appointments")]
    public async Task<ActionResult<AppointmentRecord>> Schedule(string id, AppointmentRequest request)
    {
        if (!await RoleAccess.Is(db, Uid, "ShelterStaff"))
            return StatusCode(403, new ApiError("staff_access_required", "Shelter staff access is required."));

        if (request.Type is not ("MeetAndGreet" or "HomeCheck") ||
            request.ScheduledAt <= DateTimeOffset.UtcNow ||
            string.IsNullOrWhiteSpace(request.LocationName) || string.IsNullOrWhiteSpace(request.Address))
        {
            return BadRequest(new ApiError("validation_failed", "Choose a future appointment with a location and address."));
        }

        var application = await db.Get<ApplicationRecord>("applications", id);
        if (application is null)
            return NotFound(new ApiError("application_missing", "This application does not exist."));
        if (application.Status != "Approved")
            return Conflict(new ApiError("approval_required", "Approve the application before scheduling a visit."));

        var appointment = new AppointmentRecord(
            Guid.NewGuid().ToString("N"), id, request.Type, request.ScheduledAt,
            request.LocationName.Trim(), request.Address.Trim(), request.Notes?.Trim(), "Scheduled", DateTimeOffset.UtcNow);
        await db.Put("appointments", appointment.Id, appointment);
        return Created($"/api/appointments/{appointment.Id}", appointment);
    }

    [HttpPatch("appointments/{id}")]
    public async Task<ActionResult<AppointmentRecord>> UpdateAppointment(string id, AppointmentUpdateRequest request)
    {
        if (!await RoleAccess.Is(db, Uid, "ShelterStaff"))
            return StatusCode(403, new ApiError("staff_access_required", "Shelter staff access is required."));

        if (request.Status is not ("Scheduled" or "Completed" or "Cancelled"))
            return BadRequest(new ApiError("invalid_status", "Choose a valid appointment status."));

        var appointment = await db.Get<AppointmentRecord>("appointments", id);
        if (appointment is null)
            return NotFound(new ApiError("appointment_missing", "This appointment does not exist."));

        if ((request.ScheduledAt != null || request.Status == "Scheduled") &&
            (request.ScheduledAt ?? appointment.ScheduledAt) <= DateTimeOffset.UtcNow)
            return BadRequest(new ApiError("invalid_date", "The appointment must be in the future."));
        if ((request.LocationName != null && string.IsNullOrWhiteSpace(request.LocationName)) ||
            (request.Address != null && string.IsNullOrWhiteSpace(request.Address)))
            return BadRequest(new ApiError("validation_failed", "Enter a location and address."));

        var updated = appointment with
        {
            Status = request.Status,
            ScheduledAt = request.ScheduledAt ?? appointment.ScheduledAt,
            LocationName = request.LocationName?.Trim() ?? appointment.LocationName,
            Address = request.Address?.Trim() ?? appointment.Address,
            Notes = request.Notes?.Trim() ?? appointment.Notes,
            UpdatedAt = DateTimeOffset.UtcNow
        };
        await db.Put("appointments", id, updated);
        return updated;
    }

    [HttpPost("adoptions")]
    public async Task<ActionResult<AdoptionRecord>> Finalise(AdoptionRequest request)
    {
        if (!await RoleAccess.Is(db, Uid, "ShelterStaff"))
            return StatusCode(403, new ApiError("staff_access_required", "Shelter staff access is required."));

        var application = await db.Get<ApplicationRecord>("applications", request.ApplicationId);
        if (application is null || application.Status != "Approved")
            return Conflict(new ApiError("approval_required", "Approve the application before finalising adoption."));

        if (request.CollectionDate < DateTimeOffset.UtcNow.Date ||
            request.FollowUpDueAt <= request.CollectionDate || string.IsNullOrWhiteSpace(request.CareInstructions))
        {
            return BadRequest(new ApiError("validation_failed", "Enter valid collection, care and follow-up details."));
        }

        try
        {
            var adoption = await journey.Finalise(request);
            return Created($"/api/adoptions/{adoption.Id}", adoption);
        }
        catch (JourneyConflict conflict) { return Conflict(conflict.Error); }
    }

    [HttpGet("adoptions/me")]
    public async Task<ActionResult<List<AdoptionRecord>>> MyAdoptions()
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        return (await db.All<AdoptionRecord>("adoptions")).Where(a => a.AdopterId == Uid).ToList();
    }

    [HttpPost("adoptions/{id}/follow-ups")]
    public async Task<ActionResult<FollowUpRecord>> FollowUp(string id, FollowUpRequest request)
    {
        if (!await RoleAccess.Is(db, Uid, "Adopter"))
            return StatusCode(403, new ApiError("adopter_access_required", "Adopter access is required."));

        var adoption = await db.Get<AdoptionRecord>("adoptions", id);
        if (adoption is null || adoption.AdopterId != Uid)
            return NotFound(new ApiError("adoption_missing", "This adoption does not exist."));

        if (request.WellbeingStatus is not ("Good" or "Concern" or "NeedsAssistance") ||
            request.Notes.Trim().Length is < 10 or > 1000)
        {
            return BadRequest(new ApiError("validation_failed", "Choose a wellbeing status and enter 10-1000 characters."));
        }

        if (request.PhotoUrl != null) {
            var photoId = request.PhotoUrl.StartsWith("/api/media/") ? request.PhotoUrl[11..] : "";
            var photo = await db.Get<MediaRecord>("media", photoId);
            if (photo == null || photo.OwnerId != Uid || photo.Kind != "followup" || photo.EntityId != id)
                return BadRequest(new ApiError("invalid_photo", "Upload a photo for this adoption."));
        }
        var followUp = new FollowUpRecord(
            Guid.NewGuid().ToString("N"), id, Uid, request.WellbeingStatus,
            request.Notes.Trim(), DateTimeOffset.UtcNow, request.PhotoUrl);
        await db.Put("followups", followUp.Id, followUp);
        return Created($"/api/adoptions/{id}/follow-ups/{followUp.Id}", followUp);
    }
}
