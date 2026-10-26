/**************************************************************************************************************
 * Author: Microsoft
 * Date: 2026
 * Title: Create web APIs with ASP.NET Core
 * Type: [Source Code]
 * Available at: <https://learn.microsoft.com/aspnet/core/web-api/> [Accessed 9 September 2026]
 **************************************************************************************************************/

using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using YeboPets.Api;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddSingleton<Db>();
builder.Services.AddSingleton<JourneyStore>();
builder.Services.AddSingleton<IPushSender, FirebasePush>();
builder.Services.AddSingleton<PushDispatcher>();
builder.Services.AddHostedService<PushMonitor>();
builder.Services.AddSingleton<IBlobStore, AzureBlobStore>();
builder.Services.AddSingleton<ITokens, FirebaseTokens>();

builder.Services.AddControllers().ConfigureApiBehaviorOptions(options =>
{
    options.InvalidModelStateResponseFactory = context =>
        new BadRequestObjectResult(new ApiError(
            "validation_failed",
            "Please check your input.",
            context.ModelState
                .Where(item => item.Value?.Errors.Count > 0)
                .ToDictionary(
                    item => item.Key,
                    item => item.Value!.Errors
                        .Select(_ => "This value is invalid.")
                        .ToArray()
                )
        ));
});

var app = builder.Build();

if (args.Contains("--seed"))
{
    var path = Path.Combine(app.Environment.ContentRootPath, "seed-animals.json");

    var animals = JsonSerializer.Deserialize<List<Animal>>(
        await File.ReadAllTextAsync(path),
        new JsonSerializerOptions(JsonSerializerDefaults.Web)
    ) ?? [];

    var db = app.Services.GetRequiredService<Db>();

    foreach (var animal in animals)
        await db.InsertIfMissing("animals", animal.Id, animal);

    Console.WriteLine($"Seed completed for {animals.Count} sample animals.");
    return;
}

app.Use(async (context, next) =>
{
    try
    {
        await next();
    }
    catch (OperationCanceledException)
        when (context.RequestAborted.IsCancellationRequested)
    {
    }
    catch (UnauthorizedAccessException)
    {
        context.Response.StatusCode = 401;
        context.Response.Headers.WWWAuthenticate = "Bearer";

        await context.Response.WriteAsJsonAsync(
            new ApiError("unauthenticated", "Please sign in again."));
    }
    catch (Exception exception)
    {
        app.Logger.LogError("Request failed: {Type}", exception.GetType().Name);

        if (context.Response.HasStarted)
            throw;

        context.Response.StatusCode = 503;

        await context.Response.WriteAsJsonAsync(
            new ApiError("service_unavailable", "The service is temporarily unavailable."));
    }
});

app.Use(async (context, next) =>
{
    if (context.Request.Path == "/health")
    {
        await next();
        return;
    }

    var header = context.Request.Headers.Authorization.ToString();

    if (!header.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
        throw new UnauthorizedAccessException();

    var token = header["Bearer ".Length..].Trim();

    if (token.Length == 0)
        throw new UnauthorizedAccessException();

    context.User = await context.RequestServices
        .GetRequiredService<ITokens>()
        .Verify(token);

    await next();
});

app.MapGet("/health", () => Results.Ok(new { status = "ok" }));
app.MapControllers();

app.MapFallback(() => Results.NotFound(
    new ApiError("not_found", "The requested endpoint does not exist.")));

app.Run();

public partial class Program { }
