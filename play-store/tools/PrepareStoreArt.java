import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Reproducibly exports Play Store and launcher PNGs from the checked-in masters. */
public final class PrepareStoreArt {
    private static final RenderingHints QUALITY_HINTS = new RenderingHints(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BICUBIC
    );

    static {
        QUALITY_HINTS.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        QUALITY_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        QUALITY_HINTS.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    private PrepareStoreArt() {}

    public static void main(String[] args) throws IOException {
        Path project = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        Path store = project.resolve("play-store");
        Path iconMaster = store.resolve("source/glowwords-icon-master.png");
        Path featureMaster = store.resolve("source/glowwords-feature-master.png");

        BufferedImage icon = requireImage(iconMaster);
        BufferedImage feature = requireImage(featureMaster);

        writePng(resize(icon, 512, 512), store.resolve("graphics/icon-512.png"));
        writePng(toRgb(cropAndResize(feature, 1024, 500, 0.50, 0.50)),
            store.resolve("graphics/feature-graphic-1024x500.png"));

        exportLauncherIcon(project, icon, "mipmap-mdpi", 48);
        exportLauncherIcon(project, icon, "mipmap-hdpi", 72);
        exportLauncherIcon(project, icon, "mipmap-xhdpi", 96);
        exportLauncherIcon(project, icon, "mipmap-xxhdpi", 144);
        exportLauncherIcon(project, icon, "mipmap-xxxhdpi", 192);
    }

    private static void exportLauncherIcon(Path project, BufferedImage source, String density, int size)
        throws IOException {
        Path destination = project.resolve("app/src/main/res").resolve(density).resolve("ic_launcher.png");
        writePng(resize(source, size, size), destination);
    }

    private static BufferedImage cropAndResize(
        BufferedImage source,
        int targetWidth,
        int targetHeight,
        double focalX,
        double focalY
    ) {
        double targetRatio = (double) targetWidth / targetHeight;
        double sourceRatio = (double) source.getWidth() / source.getHeight();
        int cropWidth;
        int cropHeight;

        if (sourceRatio > targetRatio) {
            cropHeight = source.getHeight();
            cropWidth = (int) Math.round(cropHeight * targetRatio);
        } else {
            cropWidth = source.getWidth();
            cropHeight = (int) Math.round(cropWidth / targetRatio);
        }

        int x = clamp((int) Math.round(source.getWidth() * focalX - cropWidth / 2.0), 0,
            source.getWidth() - cropWidth);
        int y = clamp((int) Math.round(source.getHeight() * focalY - cropHeight / 2.0), 0,
            source.getHeight() - cropHeight);
        BufferedImage crop = source.getSubimage(x, y, cropWidth, cropHeight);
        return resize(crop, targetWidth, targetHeight);
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHints(QUALITY_HINTS);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return output;
    }

    private static BufferedImage toRgb(BufferedImage source) {
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return output;
    }

    private static BufferedImage requireImage(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) throw new IOException("Unsupported image: " + path);
        return image;
    }

    private static void writePng(BufferedImage image, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if (!ImageIO.write(image, "png", path.toFile())) {
            throw new IOException("No PNG writer is available for " + path);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
