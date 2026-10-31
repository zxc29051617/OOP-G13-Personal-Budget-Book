package budgetbook.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

/** 在沒有螢幕的環境把 Swing 元件畫成圖片（不建立 JFrame）。 */
final class OffscreenRenderer {
    private OffscreenRenderer() {
    }

    static void prepare(JComponent root, int width, int height) {
        if (!root.isDisplayable()) {
            root.addNotify();
        }
        root.setSize(width, height);
        root.validate();
    }

    static BufferedImage render(JComponent root, int width, int height) {
        prepare(root, width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        root.printAll(g);
        g.dispose();
        return image;
    }

    static void save(BufferedImage image, Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        ImageIO.write(image, "png", file.toFile());
    }

    static int countDistinctColors(BufferedImage image) {
        return (int) java.util.stream.IntStream.range(0, image.getWidth() * image.getHeight())
                .filter(i -> i % 97 == 0)
                .map(i -> image.getRGB(i % image.getWidth(), i / image.getWidth()))
                .distinct().count();
    }
}
