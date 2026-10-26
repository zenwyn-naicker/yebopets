using SkiaSharp;

namespace YeboPets.Api.Tests;

internal static class ImageFixtures
{
    public static byte[] Create(SKEncodedImageFormat format = SKEncodedImageFormat.Jpeg, int width = 2, int height = 2)
    {
        using var bitmap = new SKBitmap(width, height);
        bitmap.Erase(SKColors.Coral);
        using var encoded = bitmap.Encode(format, 90);
        return encoded.ToArray();
    }
}
