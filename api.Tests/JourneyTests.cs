using System.Net;
using System.Net.Http.Json;
using YeboPets.Api;
using Xunit;

namespace YeboPets.Api.Tests;

public sealed class JourneyTests(ApiFactory factory) : IClassFixture<ApiFactory>
{
    [Fact]
    public async Task CompleteJourneyEnforcesRolesAndOwnership()
    {
        const string adopterId = "test-journey-adopter";
        const string staffId = "staff-journey-reviewer";
        const string animalId = "journey-luna";

        await factory.Database.Put("profiles", adopterId, new AdopterProfile
        {
            Phone = "+27821234567",
            Area = "Durban North",
            Housing = "House",
            Activity = "Active",
            CareReady = true
        });
        await factory.Database.Put("animals", animalId, AnimalTests.Sample(animalId));

        var adopter = factory.AsUser(adopterId);
        var staff = factory.AsUser(staffId);

        var submitted = await adopter.PostAsJsonAsync($"/api/animals/{animalId}/applications", new ApplicationRequest(
            "I can provide a secure, active and caring home for this pet.", true, true, null));
        Assert.Equal(HttpStatusCode.Created, submitted.StatusCode);
        var application = await submitted.Content.ReadFromJsonAsync<ApplicationRecord>();
        Assert.NotNull(application);

        var duplicate = await adopter.PostAsJsonAsync($"/api/animals/{animalId}/applications", new ApplicationRequest(
            "I can provide a secure, active and caring home for this pet.", true, true, null));
        Assert.Equal(HttpStatusCode.Conflict, duplicate.StatusCode);

        var forbidden = await adopter.PatchAsJsonAsync($"/api/applications/{application!.Id}/decision", new ApplicationDecisionRequest("Approved", null));
        Assert.Equal(HttpStatusCode.Forbidden, forbidden.StatusCode);

        var approved = await staff.PatchAsJsonAsync($"/api/applications/{application.Id}/decision", new ApplicationDecisionRequest("Approved", "Ready for a meet-and-greet."));
        Assert.Equal(HttpStatusCode.OK, approved.StatusCode);

        var appointment = await staff.PostAsJsonAsync($"/api/applications/{application.Id}/appointments", new AppointmentRequest(
            "MeetAndGreet", DateTimeOffset.UtcNow.AddDays(2), "Coastal Paws Rescue", "Durban North", null));
        Assert.Equal(HttpStatusCode.Created, appointment.StatusCode);

        var adoptionResponse = await staff.PostAsJsonAsync("/api/adoptions", new AdoptionRequest(
            application.Id, DateTimeOffset.UtcNow.AddDays(3), "Continue the current care routine.", null, DateTimeOffset.UtcNow.AddDays(17)));
        Assert.Equal(HttpStatusCode.Created, adoptionResponse.StatusCode);
        var adoption = await adoptionResponse.Content.ReadFromJsonAsync<AdoptionRecord>();
        Assert.NotNull(adoption);

        var followUp = await adopter.PostAsJsonAsync($"/api/adoptions/{adoption!.Id}/follow-ups", new FollowUpRequest(
            "Good", "Settling in very well and enjoying daily walks."));
        Assert.Equal(HttpStatusCode.Created, followUp.StatusCode);
    }
}