import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

/** Checks upload dimensions, alpha rules, and file-size limits for Play graphics. */
public final class VerifyStoreAssets {
    private VerifyStoreAssets() {}

    public static void main(String[] args) throws IOException {
        Path project = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        Path store = project.resolve("play-store");

        verify(store.resolve("graphics/icon-512.png"), 512, 512, true, 1_024 * 1_024);
        verify(store.resolve("graphics/feature-graphic-1024x500.png"), 1024, 500, false, Long.MAX_VALUE);

        Path screenshots = store.resolve("screenshots/phone");
        List<Path> screenshotFiles;
        try (var files = Files.list(screenshots)) {
            screenshotFiles = files
                .filter((path) -> path.getFileName().toString().endsWith(".png"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        }
        if (screenshotFiles.size() < 3 || screenshotFiles.size() > 8) {
            throw new IOException("Expected 3–8 phone screenshots, found " + screenshotFiles.size());
        }
        for (Path screenshot : screenshotFiles) {
            verify(screenshot, 1080, 1920, false, 8L * 1024 * 1024);
        }
    }

    private static void verify(
        Path path,
        int expectedWidth,
        int expectedHeight,
        boolean alphaAllowed,
        long maximumBytes
    ) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) throw new IOException("Unsupported image: " + path);
        if (image.getWidth() != expectedWidth || image.getHeight() != expectedHeight) {
            throw new IOException(path + " is " + image.getWidth() + "x" + image.getHeight()
                + "; expected " + expectedWidth + "x" + expectedHeight);
        }
        if (!alphaAllowed && image.getColorModel().hasAlpha()) {
            throw new IOException(path + " contains an alpha channel");
        }
        long bytes = Files.size(path);
        if (bytes > maximumBytes) {
            throw new IOException(path + " is " + bytes + " bytes; maximum is " + maximumBytes);
        }
        System.out.printf("OK  %s  %dx%d  alpha=%s  %,d bytes%n",
            path, image.getWidth(), image.getHeight(), image.getColorModel().hasAlpha(), bytes);
    }
}
