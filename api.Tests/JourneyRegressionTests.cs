using System.Net;
using System.Net.Http.Json;
using MongoDB.Bson;
using YeboPets.Api;
using Xunit;

namespace YeboPets.Api.Tests;

public sealed class JourneyRegressionTests(ApiFactory factory) : IClassFixture<ApiFactory>
{
    private async Task<(HttpClient Adopter, HttpClient Staff, ApplicationRecord Application)> Approved(string pet, string owner)
    {
        var adopter = factory.AsUser(owner);
        var staff = factory.AsUser("staff-regression");
        await factory.Database.Put("animals", pet, AnimalTests.Sample(pet));
        await factory.Database.Put("profiles", owner, new AdopterProfile { Phone = "+27821234567", CareReady = true });
        var response = await adopter.PostAsJsonAsync($"/api/animals/{pet}/applications",
            new ApplicationRequest("I offer a safe and caring permanent home.", true, true, null));
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        var application = (await response.Content.ReadFromJsonAsync<ApplicationRecord>())!;
        Assert.Equal(HttpStatusCode.OK, (await staff.PatchAsJsonAsync($"/api/applications/{application.Id}/decision",
            new ApplicationDecisionRequest("Approved", null))).StatusCode);
        return (adopter, staff, application);
    }

    private static AdoptionRequest Finalisation(string application) => new(application,
        DateTimeOffset.UtcNow.AddDays(2), "Continue daily walks and the recommended food.", null, DateTimeOffset.UtcNow.AddDays(14));

    [Fact]
    public async Task ConcurrentSubmissionsCreateExactlyOneActiveApplication()
    {
        const string owner = "test-concurrent-application";
        using var client = factory.AsUser(owner);
        await factory.Database.Put("animals", "concurrent-pet", AnimalTests.Sample("concurrent-pet"));
        await factory.Database.Put("profiles", owner, new AdopterProfile { Phone = "+27821234567", CareReady = true });
        var results = await Task.WhenAll(Enumerable.Range(0, 12).Select(_ => client.PostAsJsonAsync(
            "/api/animals/concurrent-pet/applications", new ApplicationRequest("I offer a safe and caring permanent home.", true, true, null))));
        Assert.Single(results, x => x.StatusCode == HttpStatusCode.Created);
        Assert.Equal(11, results.Count(x => x.StatusCode == HttpStatusCode.Conflict));
        Assert.Single(await factory.Database.All<ApplicationRecord>("applications"), x => x.AdopterId == owner);
    }

    [Fact]
    public async Task DeclinedApplicationAllowsReapplicationButCannotBeReactivatedAlongsideIt()
    {
        var (adopter, staff, application) = await Approved("reapply-pet", "test-reapply");
        await staff.PatchAsJsonAsync($"/api/applications/{application.Id}/decision", new ApplicationDecisionRequest("Declined", null));
        var response = await adopter.PostAsJsonAsync("/api/animals/reapply-pet/applications",
            new ApplicationRequest("I now offer a safe and caring permanent home.", true, true, null));
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        Assert.Equal(HttpStatusCode.Conflict, (await staff.PatchAsJsonAsync($"/api/applications/{application.Id}/decision",
            new ApplicationDecisionRequest("Approved", null))).StatusCode);
    }

    [Fact]
    public async Task RepeatedFinalisationAndLaterDecisionAreRejected()
    {
        var (_, staff, application) = await Approved("finalise-once-pet", "test-finalise-once");
        var request = Finalisation(application.Id);
        Assert.Equal(HttpStatusCode.Created, (await staff.PostAsJsonAsync("/api/adoptions", request)).StatusCode);
        Assert.Equal(HttpStatusCode.Conflict, (await staff.PostAsJsonAsync("/api/adoptions", request)).StatusCode);
        Assert.Single(await factory.Database.All<AdoptionRecord>("adoptions"), x => x.ApplicationId == application.Id);
        Assert.Equal("Adopted", (await factory.Database.Get<Animal>("animals", application.AnimalId))!.Status);
        Assert.Equal(HttpStatusCode.Conflict, (await staff.PatchAsJsonAsync($"/api/animals/{application.AnimalId}",
            AnimalTests.Sample(application.AnimalId))).StatusCode);
        Assert.Equal(HttpStatusCode.Conflict, (await staff.PatchAsJsonAsync($"/api/applications/{application.Id}/decision",
            new ApplicationDecisionRequest("Declined", null))).StatusCode);
    }

    [Fact]
    public async Task DifferentApprovedApplicationsCannotFinaliseTheSamePet()
    {
        var (_, staff, first) = await Approved("shared-pet", "test-first-owner");
        var (_, _, second) = await Approved("shared-pet", "test-second-owner");
        var responses = await Task.WhenAll(staff.PostAsJsonAsync("/api/adoptions", Finalisation(first.Id)),
            staff.PostAsJsonAsync("/api/adoptions", Finalisation(second.Id)));
        Assert.Single(responses, x => x.StatusCode == HttpStatusCode.Created);
        Assert.Single(responses, x => x.StatusCode == HttpStatusCode.Conflict);
        Assert.Single(await factory.Database.All<AdoptionRecord>("adoptions"), x => x.AnimalId == "shared-pet");
    }

    [Fact]
    public async Task FailedAnimalWriteRollsBackTheAdoptionInsert()
    {
        var (_, staff, application) = await Approved("rollback-pet", "test-rollback-adoption");
        await factory.Database.Database.RunCommandAsync<BsonDocument>(new BsonDocument
        {
            { "collMod", "animals" },
            { "validator", new BsonDocument("Value.Status", new BsonDocument("$ne", "Adopted")) }
        });
        try
        {
            Assert.Equal(HttpStatusCode.ServiceUnavailable,
                (await staff.PostAsJsonAsync("/api/adoptions", Finalisation(application.Id))).StatusCode);
            Assert.DoesNotContain(await factory.Database.All<AdoptionRecord>("adoptions"), x => x.ApplicationId == application.Id);
            Assert.Equal("Published", (await factory.Database.Get<Animal>("animals", application.AnimalId))!.Status);
        }
        finally
        {
            await factory.Database.Database.RunCommandAsync<BsonDocument>(new BsonDocument
                { { "collMod", "animals" }, { "validator", new BsonDocument() } });
        }
    }

    [Fact]
    public async Task AppointmentDetailsReschedulingAndFollowUpHistoryRespectOwnership()
    {
        var (adopter, staff, application) = await Approved("history-pet", "test-history-owner");
        using var stranger = factory.AsUser("test-history-stranger");
        var created = await staff.PostAsJsonAsync($"/api/applications/{application.Id}/appointments", new AppointmentRequest(
            "HomeCheck", DateTimeOffset.UtcNow.AddDays(2), "Adopter home", "12 Test Road", "Bring the checklist"));
        var appointment = (await created.Content.ReadFromJsonAsync<AppointmentRecord>())!;
        Assert.Equal(HttpStatusCode.Created, created.StatusCode);
        var update = new AppointmentUpdateRequest("Scheduled", DateTimeOffset.UtcNow.AddDays(3), "New location", "14 Test Road", "Updated notes");
        Assert.Equal(HttpStatusCode.Forbidden, (await adopter.PatchAsJsonAsync($"/api/appointments/{appointment.Id}", update)).StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await staff.PatchAsJsonAsync($"/api/appointments/{appointment.Id}", update)).StatusCode);
        var details = (await adopter.GetFromJsonAsync<ApplicationDetails>($"/api/applications/{application.Id}"))!;
        Assert.Equal("New location", details.Appointments.Single().LocationName);
        Assert.Equal("14 Test Road", details.Appointments.Single().Address);
        Assert.NotEqual(PushEvent.Appointment(appointment, "test-history-owner").Id,
            PushEvent.Appointment(details.Appointments.Single(), "test-history-owner").Id);
        Assert.Equal(HttpStatusCode.NotFound, (await stranger.GetAsync($"/api/applications/{application.Id}")).StatusCode);
        foreach (var status in new[] { "Completed", "Cancelled" })
            Assert.Equal(HttpStatusCode.OK, (await staff.PatchAsJsonAsync($"/api/appointments/{appointment.Id}",
                new AppointmentUpdateRequest(status, null))).StatusCode);
        var adoption = (await (await staff.PostAsJsonAsync("/api/adoptions", Finalisation(application.Id))).Content.ReadFromJsonAsync<AdoptionRecord>())!;
        Assert.Equal(HttpStatusCode.Created, (await adopter.PostAsJsonAsync($"/api/adoptions/{adoption.Id}/follow-ups",
            new FollowUpRequest("Good", "Settling in very well with the family."))).StatusCode);
        Assert.Single((await adopter.GetFromJsonAsync<List<FollowUpRecord>>($"/api/adoptions/{adoption.Id}/follow-ups"))!);
        Assert.Single((await staff.GetFromJsonAsync<List<FollowUpRecord>>($"/api/adoptions/{adoption.Id}/follow-ups"))!);
        Assert.Equal(HttpStatusCode.NotFound, (await stranger.GetAsync($"/api/adoptions/{adoption.Id}/follow-ups")).StatusCode);
        Assert.Single((await staff.GetFromJsonAsync<ApplicationDetails>($"/api/applications/{application.Id}"))!.FollowUps);
    }
}
