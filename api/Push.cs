/**************************************************************************************************************
 * Author: Firebase
 * Date: 2026
 * Title: Admin SDK message sending
 * Type: [Source Code]
 * Available at: <https://firebase.google.com/docs/cloud-messaging/send/admin-sdk> [Accessed 20 October 2026]
 **************************************************************************************************************/

using System.ComponentModel.DataAnnotations;
using System.Security.Cryptography;
using System.Text;
using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Google.Apis.Auth.OAuth2;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Bson.Serialization.Attributes;
using MongoDB.Driver;

namespace YeboPets.Api;
public sealed class PushDevice {
    [BsonId] public string Id { get; set; } = "";
    public string Uid { get; set; } = "";
    public string Token { get; set; } = "";
    public DateTime UpdatedAt { get; set; }
}
public record DeviceRequest([Required, StringLength(4096, MinimumLength = 10)] string Token);
public record PushEvent(string Id, string Uid, string Kind)
{
    public static PushEvent Appointment(AppointmentRecord visit, string uid) =>
        new($"visit:{visit.Id}:{visit.UpdatedAt?.UtcTicks ?? visit.ScheduledAt.UtcTicks}:{visit.Status}", uid, "appointment");
}
public interface IPushSender { Task Send(string token, PushEvent value, CancellationToken ct); }
public sealed class FirebasePush(IConfiguration config) : IPushSender {
    private readonly Lazy<FirebaseMessaging> messaging = new(() => {
        var app = FirebaseApp.Create(new AppOptions {
            Credential = CredentialFactory.FromJson<ServiceAccountCredential>(
                config["Firebase:ServiceAccountJson"] ?? throw new InvalidOperationException("Firebase missing"))
                .ToGoogleCredential(), ProjectId = config["Firebase:ProjectId"]
        }, "yebopets-push");
        return FirebaseMessaging.GetMessaging(app);
    });
    public async Task Send(string token, PushEvent value, CancellationToken ct) {
        await messaging.Value.SendAsync(new Message {
            Token = token,
            Data = new Dictionary<string,string> {
                ["uid"] = value.Uid, ["eventId"] = value.Id, ["kind"] = value.Kind
            },
            Android = new AndroidConfig { Priority = Priority.High, TimeToLive = TimeSpan.FromHours(24) }
        }, ct);
    }
}
[Route("api/me/devices")]
public sealed class DevicesController(Db db) : ApiController {
    public static string Key(string token) => Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(token)));
    [HttpPut]
    public async Task<IActionResult> Register(DeviceRequest request) {

        await db.Database.GetCollection<PushDevice>("devices").ReplaceOneAsync(
            x => x.Id == Key(request.Token), new PushDevice {
                Id = Key(request.Token), Token = request.Token, Uid = Uid, UpdatedAt = DateTime.UtcNow
            }, new ReplaceOptions { IsUpsert = true });
        return NoContent();
    }
    [HttpDelete]
    public async Task<IActionResult> Remove(DeviceRequest request) {
        await db.Database.GetCollection<PushDevice>("devices").DeleteOneAsync(
            x => x.Id == Key(request.Token) && x.Uid == Uid);
        return NoContent();
    }
}
public sealed class PushDispatcher(Db db, IPushSender sender) {
    public async Task Deliver(PushEvent value, CancellationToken ct) {
        var settings = await db.Get<UserSettings>("settings", value.Uid) ?? new();
        if (!settings.NotificationsEnabled ||
            (value.Kind == "status" && !settings.StatusAlertsEnabled) ||
            (value.Kind == "appointment" && !settings.AppointmentRemindersEnabled)) return;
        var devices = db.Database.GetCollection<PushDevice>("devices");
        foreach (var device in await devices.Find(x => x.Uid == value.Uid &&
                     x.UpdatedAt > DateTime.UtcNow.AddDays(-60)).ToListAsync(ct)) {
            var receipt = DevicesController.Key(value.Id + device.Id);
            if (await db.Get<string>("pushReceipts", receipt) != null) continue;
            try {
                await sender.Send(device.Token, value, ct);
                await db.Put("pushReceipts", receipt, value.Id);
            } catch (FirebaseMessagingException e) when (e.MessagingErrorCode == MessagingErrorCode.Unregistered) {
                await devices.DeleteOneAsync(x => x.Id == device.Id && x.Token == device.Token, ct);
            }
        }
    }
}
public sealed class PushMonitor(IServiceScopeFactory scopes, IConfiguration config, ILogger<PushMonitor> log)
    : BackgroundService {
    protected override async Task ExecuteAsync(CancellationToken stoppingToken) {
        if (!config.GetValue<bool>("Push:Enabled")) return;
        while (!stoppingToken.IsCancellationRequested) {
            try {
                using var scope = scopes.CreateScope();
                var db = scope.ServiceProvider.GetRequiredService<Db>();
                var push = scope.ServiceProvider.GetRequiredService<PushDispatcher>();
                var apps = await db.All<ApplicationRecord>("applications");
                foreach (var a in apps.Where(x => x.Status != "Submitted"))
                    await push.Deliver(new($"decision:{a.Id}:{a.UpdatedAt.UtcTicks}", a.AdopterId, "status"), stoppingToken);
                foreach (var a in await db.All<AppointmentRecord>("appointments")) {
                    var owner = apps.Find(x => x.Id == a.ApplicationId)?.AdopterId;
                    if (owner == null) continue;
                    await push.Deliver(PushEvent.Appointment(a, owner), stoppingToken);
                    if (a.Status == "Scheduled" && a.ScheduledAt > DateTimeOffset.UtcNow && a.ScheduledAt <= DateTimeOffset.UtcNow.AddDays(1))
                        await push.Deliver(new($"reminder:{a.Id}:{a.ScheduledAt.UtcTicks}", owner, "appointment"), stoppingToken);
                }
                foreach (var a in await db.All<AdoptionRecord>("adoptions"))
                    if (a.FollowUpDueAt <= DateTimeOffset.UtcNow.AddDays(1) &&
                        !(await db.All<FollowUpRecord>("followups")).Any(f => f.AdoptionId == a.Id))
                        await push.Deliver(new($"followup:{a.Id}:{a.FollowUpDueAt.UtcTicks}", a.AdopterId, "followup"), stoppingToken);
            } catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested) { break; }
              catch (Exception e) { log.LogWarning("Push scan failed: {Type}", e.GetType().Name); }
            await Task.Delay(TimeSpan.FromSeconds(15), stoppingToken);
        }
    }
}
