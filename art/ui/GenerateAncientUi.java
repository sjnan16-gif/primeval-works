import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GenerateAncientUi {
    private static final int CLEAR = 0x00000000;
    private static final int INK = 0xFF292128;
    private static final int EARTH = 0xFF5A3D39;
    private static final int OCHRE = 0xFFA56F49;
    private static final int PAPER = 0xFFD7B87A;

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args.length == 0
                ? "src/main/resources/assets/primevalworks/textures/gui"
                : args[0]);
        Files.createDirectories(output);
        ImageIO.write(panel(), "png", output.resolve("ancient_companion_panel.png").toFile());
        ImageIO.write(button(false), "png", output.resolve("ancient_button.png").toFile());
        ImageIO.write(button(true), "png", output.resolve("ancient_button_hovered.png").toFile());
        ImageIO.write(slot(false), "png", output.resolve("ancient_slot.png").toFile());
        ImageIO.write(slot(true), "png", output.resolve("ancient_slot_glow.png").toFile());
    }

    private static BufferedImage panel() {
        BufferedImage image = image(256, 128);
        fill(image, 7, 2, 242, 124, EARTH);
        fill(image, 3, 8, 250, 112, EARTH);
        fill(image, 5, 5, 246, 117, EARTH);
        cut(image, 3, 8, 3, 4);
        cut(image, 6, 3, 8, 2);
        cut(image, 241, 3, 9, 2);
        cut(image, 250, 8, 3, 5);
        cut(image, 3, 108, 3, 8);
        cut(image, 6, 120, 10, 4);
        cut(image, 242, 120, 7, 4);
        cut(image, 250, 110, 3, 6);

        fill(image, 8, 5, 233, 3, OCHRE);
        fill(image, 5, 11, 3, 98, OCHRE);
        fill(image, 14, 3, 37, 2, OCHRE);
        fill(image, 61, 4, 72, 2, OCHRE);
        fill(image, 146, 3, 43, 2, OCHRE);
        fill(image, 204, 5, 37, 2, OCHRE);
        fill(image, 9, 120, 70, 4, INK);
        fill(image, 87, 122, 58, 3, INK);
        fill(image, 154, 120, 91, 4, INK);
        fill(image, 248, 15, 4, 94, INK);
        fill(image, 244, 111, 5, 8, INK);

        fill(image, 13, 13, 230, 101, PAPER);
        fill(image, 10, 17, 236, 91, PAPER);
        fill(image, 15, 10, 44, 5, PAPER);
        fill(image, 65, 11, 31, 4, PAPER);
        fill(image, 104, 9, 39, 6, PAPER);
        fill(image, 151, 11, 56, 4, PAPER);
        fill(image, 215, 13, 28, 4, PAPER);
        fill(image, 15, 112, 39, 4, PAPER);
        fill(image, 61, 110, 51, 6, PAPER);
        fill(image, 121, 112, 28, 4, PAPER);
        fill(image, 158, 110, 45, 6, PAPER);
        fill(image, 211, 112, 31, 4, PAPER);

        fill(image, 10, 21, 3, 28, OCHRE);
        fill(image, 11, 56, 2, 45, OCHRE);
        fill(image, 15, 13, 44, 2, OCHRE);
        fill(image, 68, 14, 28, 1, OCHRE);
        fill(image, 105, 14, 38, 1, OCHRE);
        fill(image, 154, 14, 52, 1, OCHRE);
        fill(image, 216, 16, 27, 2, OCHRE);
        fill(image, 17, 113, 36, 2, OCHRE);
        fill(image, 63, 114, 48, 2, OCHRE);
        fill(image, 122, 113, 26, 2, OCHRE);
        fill(image, 159, 114, 42, 2, OCHRE);
        fill(image, 214, 113, 27, 2, OCHRE);

        int[][] fibers = {
                {24, 24, 4, 1}, {29, 25, 2, 1}, {38, 27, 3, 1}, {70, 21, 5, 1},
                {77, 22, 3, 1}, {101, 29, 4, 1}, {128, 20, 5, 1}, {135, 21, 2, 1},
                {174, 27, 4, 1}, {180, 26, 3, 1}, {214, 22, 5, 1}, {229, 31, 3, 1},
                {19, 65, 3, 1}, {50, 57, 5, 1}, {57, 58, 2, 1}, {82, 67, 4, 1},
                {115, 60, 5, 1}, {122, 61, 3, 1}, {153, 71, 4, 1}, {188, 62, 5, 1},
                {195, 61, 2, 1}, {221, 73, 4, 1}, {31, 94, 5, 1}, {38, 95, 3, 1},
                {66, 101, 4, 1}, {97, 91, 5, 1}, {137, 99, 4, 1}, {143, 100, 3, 1},
                {181, 93, 4, 1}, {208, 102, 5, 1}, {215, 101, 2, 1}, {42, 34, 2, 3},
                {92, 46, 1, 4}, {167, 39, 2, 2}, {202, 48, 1, 5}, {58, 81, 2, 2},
                {119, 84, 1, 3}, {232, 88, 2, 2}
        };
        for (int[] mark : fibers) {
            fill(image, mark[0], mark[1], mark[2], mark[3], OCHRE);
        }

        fossil(image, 31, 74);
        fern(image, 220, 75);
        pin(image, 8, 9);
        pin(image, 238, 9);
        pin(image, 8, 109);
        pin(image, 238, 109);
        return image;
    }

    private static BufferedImage button(boolean hovered) {
        BufferedImage image = image(64, 20);
        int center = hovered ? PAPER : OCHRE;
        fill(image, 3, 1, 58, 18, INK);
        fill(image, 1, 4, 62, 12, INK);
        fill(image, 3, 2, 58, 14, EARTH);
        fill(image, 2, 5, 60, 9, EARTH);
        fill(image, 5, 4, 54, 10, center);
        fill(image, 4, 5, 56, 7, center);
        fill(image, 7, 3, 18, 1, PAPER);
        fill(image, 30, 3, 24, 1, PAPER);
        fill(image, 9, 14, 17, 2, INK);
        fill(image, 32, 14, 24, 2, INK);
        cut(image, 1, 4, 2, 2);
        cut(image, 61, 14, 2, 2);
        return image;
    }

    private static BufferedImage slot(boolean glowing) {
        BufferedImage image = image(20, 20);
        int rim = glowing ? PAPER : OCHRE;
        fill(image, 2, 1, 16, 18, rim);
        fill(image, 1, 3, 18, 14, rim);
        fill(image, 3, 3, 14, 14, INK);
        fill(image, 4, 4, 12, 12, EARTH);
        fill(image, 5, 5, 10, 10, glowing ? OCHRE : EARTH);
        fill(image, 3, 3, 11, 1, PAPER);
        fill(image, 3, 4, 1, 10, PAPER);
        fill(image, 5, 16, 11, 1, INK);
        fill(image, 16, 5, 1, 11, INK);
        cut(image, 1, 3, 2, 2);
        cut(image, 17, 15, 2, 2);
        return image;
    }

    private static void fossil(BufferedImage image, int x, int y) {
        int[][] pixels = {
                {0, 0}, {1, 0}, {2, 0}, {3, 1}, {4, 2}, {4, 3}, {3, 4}, {2, 5},
                {1, 5}, {0, 4}, {-1, 3}, {-1, 2}, {0, 1}, {1, 1}, {2, 1}, {3, 2},
                {3, 3}, {2, 4}, {1, 4}, {0, 3}, {0, 2}, {1, 2}, {2, 2}, {2, 3}, {1, 3}
        };
        for (int[] pixel : pixels) {
            image.setRGB(x + pixel[0], y + pixel[1], OCHRE);
        }
    }

    private static void fern(BufferedImage image, int x, int y) {
        for (int i = 0; i < 13; i++) {
            image.setRGB(x, y + i, OCHRE);
            if (i > 1 && i < 11) {
                int reach = i % 3 == 0 ? 4 : 3;
                for (int j = 1; j <= reach; j++) {
                    image.setRGB(x - j, y + i - Math.min(j, 2), OCHRE);
                    image.setRGB(x + j, y + i - Math.min(j, 2), OCHRE);
                }
            }
        }
    }

    private static void pin(BufferedImage image, int x, int y) {
        fill(image, x, y, 5, 5, INK);
        fill(image, x + 1, y + 1, 3, 3, OCHRE);
        image.setRGB(x + 1, y + 1, PAPER);
    }

    private static BufferedImage image(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        fill(image, 0, 0, width, height, CLEAR);
        return image;
    }

    private static void cut(BufferedImage image, int x, int y, int width, int height) {
        fill(image, x, y, width, height, CLEAR);
    }

    private static void fill(BufferedImage image, int x, int y, int width, int height, int color) {
        for (int py = Math.max(0, y); py < Math.min(image.getHeight(), y + height); py++) {
            for (int px = Math.max(0, x); px < Math.min(image.getWidth(), x + width); px++) {
                image.setRGB(px, py, color);
            }
        }
    }
}
