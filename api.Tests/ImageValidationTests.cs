using SkiaSharp;
using YeboPets.Api;
using Xunit;

namespace YeboPets.Api.Tests;

public sealed class ImageValidationTests
{
    [Theory]
    [InlineData(SKEncodedImageFormat.Jpeg)]
    [InlineData(SKEncodedImageFormat.Png)]
    public void CompleteImagesAreAccepted(SKEncodedImageFormat format) =>
        Assert.True(MediaController.IsValidImage(ImageFixtures.Create(format)));

    [Fact]
    public void CorruptAndOversizedDimensionsAreRejected()
    {
        Assert.False(MediaController.IsValidImage([255, 216, 255, 224, 1, 2, 3, 4]));
        Assert.False(MediaController.IsValidImage([137, 80, 78, 71, 13, 10, 26, 10]));
        Assert.False(MediaController.IsValidImage(ImageFixtures.Create(SKEncodedImageFormat.Png, 4097, 1)));
    }
}
