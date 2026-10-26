using System.Net;
using System.Net.Http.Json;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using System.Net.Http.Headers;
using MongoDB.Driver;
using YeboPets.Api;
using Xunit;
namespace YeboPets.Api.Tests;
public sealed class Part3Tests(ApiFactory factory):IClassFixture<ApiFactory> {
    private static string Op()=>Guid.NewGuid().ToString("N");
    [Fact] public async Task OldReplayCannotUndoNewerRemoval() {
        var uid="test-replay"; var client=factory.AsUser(uid);
        await factory.Database.Put("animals","replay-pet",AnimalTests.Sample("replay-pet"));
        var save=new SavedOperation(Op(),"replay-pet",true);
        Assert.Equal(HttpStatusCode.OK,(await client.PostAsJsonAsync("/api/sync",save)).StatusCode);
        Assert.Equal(HttpStatusCode.OK,(await client.PostAsJsonAsync("/api/sync",new SavedOperation(Op(),"replay-pet",false))).StatusCode);
        var response=await client.PostAsJsonAsync("/api/sync",save);
        Assert.True((await response.Content.ReadFromJsonAsync<SyncReply>())!.Replayed);
        Assert.Empty((await client.GetFromJsonAsync<List<Animal>>("/api/adopters/me/saved"))!);
    }
    [Fact] public async Task SameIdWithDifferentPayloadIsRejected() {
        var client=factory.AsUser("test-mismatch"); var id=Op();
        await client.PostAsJsonAsync("/api/sync",new SavedOperation(id,"a",false));
        var reply=await client.PostAsJsonAsync("/api/sync",new SavedOperation(id,"b",false));
        Assert.Equal(HttpStatusCode.Conflict,reply.StatusCode);
    }
    [Fact] public async Task ConcurrentDuplicateHasOneMutation() {
        var client=factory.AsUser("test-concurrent");
        await factory.Database.Put("animals","parallel-pet",AnimalTests.Sample("parallel-pet"));
        var op=new SavedOperation(Op(),"parallel-pet",true);
        var responses=await Task.WhenAll(Enumerable.Range(0,12).Select(_=>client.PostAsJsonAsync("/api/sync",op)));
        Assert.All(responses,r=>Assert.Equal(HttpStatusCode.OK,r.StatusCode));
        Assert.Single((await client.GetFromJsonAsync<List<Animal>>("/api/adopters/me/saved"))!);
        var row=await factory.Database.Database.GetCollection<SavedRecord>("saved").Find(x=>x.Id=="test-concurrent").FirstAsync();
        Assert.Single(row.Operations);
    }
    [Fact] public async Task OperationIdsAreAccountScoped() {
        var id=Op();
        Assert.Equal(HttpStatusCode.OK,(await factory.AsUser("test-isolate-a").PostAsJsonAsync("/api/sync",new SavedOperation(id,"a",false))).StatusCode);
        Assert.Equal(HttpStatusCode.OK,(await factory.AsUser("test-isolate-b").PostAsJsonAsync("/api/sync",new SavedOperation(id,"b",false))).StatusCode);
    }
    [Fact] public async Task SyncChecksRoleAvailabilityAndInput() {
        Assert.Equal(HttpStatusCode.Unauthorized,(await factory.CreateClient().PostAsJsonAsync("/api/sync",new SavedOperation(Op(),"a",true))).StatusCode);
        Assert.Equal(HttpStatusCode.Forbidden,(await factory.AsUser("staff-sync").PostAsJsonAsync("/api/sync",new SavedOperation(Op(),"a",true))).StatusCode);
        var adopter=factory.AsUser("test-sync-validation");
        Assert.Equal(HttpStatusCode.BadRequest,(await adopter.PostAsJsonAsync("/api/sync",new SavedOperation("bad.id","a",false))).StatusCode);
        Assert.Equal(HttpStatusCode.NotFound,(await adopter.PostAsJsonAsync("/api/sync",new SavedOperation(Op(),"missing",true))).StatusCode);
    }
    [Fact] public async Task SettingsRoundTripAndValidation() {
        var client=factory.AsUser("test-p3-settings");
        var settings=new UserSettings {Language="zu",NotificationsEnabled=false,SyncOnMobileData=true,BiometricsEnabled=true};
        Assert.Equal(HttpStatusCode.OK,(await client.PutAsJsonAsync("/api/me/settings",settings)).StatusCode);
        Assert.Equal(settings,await client.GetFromJsonAsync<UserSettings>("/api/me/settings"));
        Assert.Equal(HttpStatusCode.BadRequest,(await client.PutAsJsonAsync("/api/me/settings",settings with {Language="xx"})).StatusCode);
    }
    [Fact] public async Task TokenRebindAndUnregisterRespectCurrentOwner() {
        var a=factory.AsUser("test-device-a"); var b=factory.AsUser("test-device-b");
        var token=new DeviceRequest("test-device-token-12345");
        await a.PutAsJsonAsync("/api/me/devices",token); await b.PutAsJsonAsync("/api/me/devices",token);
        await a.SendAsync(new HttpRequestMessage(HttpMethod.Delete,"/api/me/devices") {Content=JsonContent.Create(token)});
        var devices=factory.Database.Database.GetCollection<PushDevice>("devices");
        var row=await devices.Find(x=>x.Id==DevicesController.Key(token.Token)).FirstAsync();
        Assert.Equal("test-device-b",row.Uid);
        await b.SendAsync(new HttpRequestMessage(HttpMethod.Delete,"/api/me/devices") {Content=JsonContent.Create(token)});
        Assert.Null(await devices.Find(x=>x.Id==row.Id).FirstOrDefaultAsync());
    }
    private sealed class FakePush:IPushSender {
        public int Calls; public bool Fail;
        public Task Send(string token,PushEvent value,CancellationToken ct) {Calls++; if(Fail)throw new IOException(); return Task.CompletedTask;}
    }
    [Fact] public async Task PushRetriesFailureAndDeduplicatesSuccess() {
        var uid="test-push-retry";
        await factory.AsUser(uid).PutAsJsonAsync("/api/me/devices",new DeviceRequest("push-test-retry-token"));
        var fake=new FakePush{Fail=true}; var dispatcher=new PushDispatcher(factory.Database,fake);
        var value=new PushEvent(Op(),uid,"status");
        await Assert.ThrowsAsync<IOException>(()=>dispatcher.Deliver(value,default));
        fake.Fail=false;
        await dispatcher.Deliver(value,default); await dispatcher.Deliver(value,default);
        Assert.Equal(2,fake.Calls);
    }
    [Fact] public async Task PushHonoursCategoryAndMasterPreferences() {
        var uid="test-push-off";
        await factory.AsUser(uid).PutAsJsonAsync("/api/me/devices",new DeviceRequest("push-test-disabled-token"));
        var fake=new FakePush(); var dispatcher=new PushDispatcher(factory.Database,fake);
        await factory.Database.Put("settings",uid,new UserSettings{StatusAlertsEnabled=false,AppointmentRemindersEnabled=false});
        await dispatcher.Deliver(new(Op(),uid,"status"),default);
        await dispatcher.Deliver(new(Op(),uid,"appointment"),default);
        Assert.Equal(0,fake.Calls);
        await factory.Database.Put("settings",uid,new UserSettings{NotificationsEnabled=false});
        await dispatcher.Deliver(new(Op(),uid,"followup"),default);
        Assert.Equal(0,fake.Calls);
    }
    [Theory]
    [InlineData(new byte[]{137,80,78,71,13,10,26,10},"image/png")]
    [InlineData(new byte[]{255,216,255,224},"image/jpeg")]
    [InlineData(new byte[]{60,115,99,114,105,112,116,62},null)]
    public void MediaSniffingRejectsNonImages(byte[] value,string? expected)=>Assert.Equal(expected,MediaController.ImageType(value));
    [Fact] public async Task UnownedPhotoUploadNeverReachesStorage() {
        var client=factory.AsUser("test-media-outsider");
        using var form=new MultipartFormDataContent();
        form.Add(new StringContent("followup"),"kind");form.Add(new StringContent("not-owned"),"entityId");
        form.Add(new ByteArrayContent(new byte[]{255,216,255,1,2,3,4,5}),"file","image.jpg");
        Assert.Equal(HttpStatusCode.NotFound,(await client.PostAsync("/api/media",form)).StatusCode);
    }
    [Fact] public async Task PrivatePhotoReadDoesNotLeakToOtherAdopter() {
        await factory.Database.Put("media","private-image",new MediaRecord("private-image","test-media-owner","followup","owned","image/jpeg","https://private.invalid/blob"));
        Assert.Equal(HttpStatusCode.NotFound,(await factory.AsUser("test-media-other").GetAsync("/api/media/private-image")).StatusCode);
    }

    private sealed class MemoryBlobs:IBlobStore {
        public readonly Dictionary<string,byte[]> Data=new();
        public Task<string> Put(string id,byte[] bytes,string type,CancellationToken ct) {Data[id]=bytes;return Task.FromResult("https://private.invalid/"+id);}
        public Task<Stream> Read(string id,CancellationToken ct)=>Task.FromResult<Stream>(new MemoryStream(Data[id]));
    }
    [Fact] public async Task StaffUploadPersistsPrivateUrlAndSurvivesAnimalEdit() {
        var blobs=new MemoryBlobs();
        using var host=factory.WithWebHostBuilder(builder=>builder.ConfigureServices(services=>{
            services.RemoveAll<IBlobStore>();services.AddSingleton<IBlobStore>(blobs);
        }));
        using var account=factory.AsUser("staff-media-success");
        var pet=AnimalTests.Sample("media-pet");
        await factory.Database.Put("animals",pet.Id,pet);
        using var client=host.CreateClient();
        client.DefaultRequestHeaders.Authorization=new AuthenticationHeaderValue("Bearer","staff-media-success");
        using var form=new MultipartFormDataContent();
        form.Add(new StringContent("animal"),"kind");form.Add(new StringContent(pet.Id),"entityId");
        var bytes=ImageFixtures.Create();
        form.Add(new ByteArrayContent(bytes),"file","photo.jpg");
        var response=await client.PostAsync("/api/media",form);
        Assert.Equal(HttpStatusCode.Created,response.StatusCode);
        var uploaded=(await response.Content.ReadFromJsonAsync<MediaReply>())!;
        Assert.Equal(bytes,await client.GetByteArrayAsync(uploaded.Url));
        var stored=await factory.Database.Get<MediaRecord>("media",uploaded.Id);
        Assert.StartsWith("https://private.invalid/",stored!.BlobUrl);
        await client.PatchAsJsonAsync("/api/animals/"+pet.Id,pet with {Description="Updated description",PhotoUrl="https://untrusted.invalid/image"});
        Assert.Equal(uploaded.Url,(await factory.Database.Get<Animal>("animals",pet.Id))!.PhotoUrl);
    }
    [Fact] public async Task PhotoReferenceMustBelongToSameAdoption() {
        var uid="test-followup-photo";var client=factory.AsUser(uid);
        await factory.Database.Put("adoptions","photo-adoption",new AdoptionRecord(
            "photo-adoption","app","pet",uid,DateTimeOffset.UtcNow,DateTimeOffset.UtcNow,"Care",null,DateTimeOffset.UtcNow.AddDays(7)));
        await factory.Database.Put("media","wrong-adoption-image",new MediaRecord("wrong-adoption-image",uid,"followup","different-adoption","image/jpeg","https://private.invalid/image"));
        var invalid=await client.PostAsJsonAsync("/api/adoptions/photo-adoption/follow-ups",
            new FollowUpRequest("Good","Settling into the home very well.","/api/media/wrong-adoption-image"));
        Assert.Equal(HttpStatusCode.BadRequest,invalid.StatusCode);
        await factory.Database.Put("media","right-adoption-image",new MediaRecord("right-adoption-image",uid,"followup","photo-adoption","image/jpeg","https://private.invalid/image"));
        var valid=await client.PostAsJsonAsync("/api/adoptions/photo-adoption/follow-ups",
            new FollowUpRequest("Good","Settling into the home very well.","/api/media/right-adoption-image"));
        Assert.Equal(HttpStatusCode.Created,valid.StatusCode);
        Assert.Equal("/api/media/right-adoption-image",(await valid.Content.ReadFromJsonAsync<FollowUpRecord>())!.PhotoUrl);
    }

    [Fact] public void LanguageChangesExplanationsWithoutChangingScores() {
        var profile=new AdopterProfile {Phone="+27821234567",Area="Durban North",HasSecureYard=true,Activity="Active",Experience=2,CareReady=true};
        var animal=AnimalTests.Sample();var english=Matching.Calculate(profile,animal);
        var zulu=MatchLanguage.Apply(english,profile,animal,"zu");
        Assert.Equal(english.OverallScore,zulu.OverallScore);
        Assert.Equal(english.Factors.Select(f=>f.Points),zulu.Factors.Select(f=>f.Points));
        Assert.Equal("Abasekhaya",zulu.Factors[0].Name);
        Assert.NotEqual(english.Factors[1].Reason,zulu.Factors[1].Reason);
        Assert.Same(english,MatchLanguage.Apply(english,profile,animal,"en"));
    }
}
