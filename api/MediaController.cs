/**************************************************************************************************************
 * Author: Microsoft
 * Date: 2026
 * Title: Upload a Blob with .NET
 * Type: [Source Code]
 * Available at: <https://learn.microsoft.com/azure/storage/blobs/storage-blob-upload> [Accessed 20 October 2026]
 **************************************************************************************************************/
using Azure.Storage.Blobs;
using Azure.Storage.Blobs.Models;
using Microsoft.AspNetCore.Mvc;
using SkiaSharp;
using MongoDB.Driver;
namespace YeboPets.Api;

public record MediaRecord(string Id, string OwnerId, string Kind, string EntityId, string ContentType, string BlobUrl);
public record MediaReply(string Id, string Url);
public interface IBlobStore
{
    Task<string> Put(string id, byte[] bytes, string type, CancellationToken ct);
    Task<Stream> Read(string id, CancellationToken ct);
}
public sealed class AzureBlobStore(IConfiguration config) : IBlobStore
{
    private BlobContainerClient Container => new(config["Blob:ConnectionString"] ??
        throw new InvalidOperationException("Blob configuration missing"), config["Blob:Container"] ?? "yebopets-media");
    public async Task<string> Put(string id, byte[] bytes, string type, CancellationToken ct)
    {
        var blob = Container.GetBlobClient(id);
        await blob.UploadAsync(new MemoryStream(bytes), new BlobUploadOptions
        {
            HttpHeaders = new BlobHttpHeaders { ContentType = type, CacheControl = "private, no-store" }
        }, ct);
        return blob.Uri.AbsoluteUri;
    }
    public Task<Stream> Read(string id, CancellationToken ct) => Container.GetBlobClient(id).OpenReadAsync(cancellationToken: ct);
}
[Route("api/media")]
public sealed class MediaController(Db db, IBlobStore blobs) : ApiController
{
    public static string? ImageType(byte[] b) => b.Length >= 8 && b.Take(8).SequenceEqual(new byte[] { 137, 80, 78, 71, 13, 10, 26, 10 })
        ? "image/png" : b.Length >= 3 && b[0] == 255 && b[1] == 216 && b[2] == 255 ? "image/jpeg" : null;

    // Bound decoded memory before allocation, then require a complete JPEG/PNG pixel decode.
    public static bool IsValidImage(byte[] bytes)
    {
        if (ImageType(bytes) == null) return false;
        using var stream = new SKMemoryStream(bytes);
        using var codec = SKCodec.Create(stream);
        if (codec == null || codec.EncodedFormat is not (SKEncodedImageFormat.Jpeg or SKEncodedImageFormat.Png)) return false;
        var info = codec.Info;
        if (info.Width is < 1 or > 4096 || info.Height is < 1 or > 4096 || (long)info.Width * info.Height > 16_000_000)
            return false;
        using var bitmap = new SKBitmap(info);
        return codec.GetPixels(info, bitmap.GetPixels()) == SKCodecResult.Success;
    }
    [HttpPost, RequestSizeLimit(5_500_000)]
    public async Task<ActionResult<MediaReply>> Upload([FromForm] string kind, [FromForm] string entityId,
        IFormFile file, CancellationToken ct)
    {
        Animal? animal = null;
        if (kind == "animal")
        {
            if (!await RoleAccess.Is(db, Uid, "ShelterStaff")) return StatusCode(403);
            animal = await db.Get<Animal>("animals", entityId);
            if (animal == null) return NotFound();
        }
        else if (kind == "followup")
        {
            var adoption = await db.Get<AdoptionRecord>("adoptions", entityId);
            if (adoption?.AdopterId != Uid) return NotFound();
        }
        else return BadRequest(new ApiError("invalid_media_target", "Choose an animal or owned adoption."));
        if (file.Length is < 8 or > 5_000_000) return BadRequest(new ApiError("image_size", "Use an image smaller than 5 MB."));
        await using var memory = new MemoryStream();
        await file.CopyToAsync(memory, ct);
        var bytes = memory.ToArray();
        var type = ImageType(bytes);
        if (type == null || !IsValidImage(bytes))
            return BadRequest(new ApiError("image_type", "Use a complete JPEG or PNG up to 4096 pixels per side and 16 megapixels."));
        var id = Guid.NewGuid().ToString("N");
        var blobUrl = await blobs.Put(id, bytes, type, ct);
        var media = new MediaRecord(id, Uid, kind, entityId, type, blobUrl);
        await db.Put("media", id, media);
        var url = "/api/media/" + id;
        if (animal != null)
            await db.Database.GetCollection<Entry<Animal>>("animals").UpdateOneAsync(x => x.Id == entityId,
                Builders<Entry<Animal>>.Update.Set(x => x.Value.PhotoUrl, url), cancellationToken: ct);
        return Created(url, new MediaReply(id, url));
    }
    [HttpGet("{id}")]
    public async Task<IActionResult> Download(string id, CancellationToken ct)
    {
        var media = await db.Get<MediaRecord>("media", id);
        if (media == null) return NotFound();
        var staff = await RoleAccess.Is(db, Uid, "ShelterStaff");
        if (media.Kind == "followup" && media.OwnerId != Uid && !staff) return NotFound();
        if (media.Kind == "animal" && !staff &&
            (await db.Get<Animal>("animals", media.EntityId))?.Status != "Published") return NotFound();
        Response.Headers.CacheControl = "private, no-store";
        Response.Headers.XContentTypeOptions = "nosniff";
        return File(await blobs.Read(id, ct), media.ContentType);
    }
}
