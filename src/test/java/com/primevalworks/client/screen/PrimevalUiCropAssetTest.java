package com.primevalworks.client.screen;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrimevalUiCropAssetTest {
    private static final Path ASSET = Path.of(
            "src/main/resources/assets/primevalworks/textures/gui/ui_crop.png");

    @Test
    void authoredAtlasKeepsEveryNamedCropIsolated() throws Exception {
        BufferedImage image = ImageIO.read(ASSET.toFile());
        assertEquals(PrimevalUiCrop.TEXTURE_WIDTH, image.getWidth());
        assertEquals(PrimevalUiCrop.TEXTURE_HEIGHT, image.getHeight());

        List<Rect> crops = List.of(
                new Rect(209, 23, 195, 74),
                new Rect(84, 42, 25, 26),
                new Rect(120, 50, 25, 11),
                new Rect(151, 49, 53, 15),
                new Rect(80, 78, 29, 15),
                new Rect(117, 75, 18, 19),
                new Rect(140, 79, 10, 11),
                new Rect(155, 88, 2, 33),
                new Rect(162, 104, 44, 3),
                new Rect(80, 105, 69, 6),
                new Rect(81, 145, 18, 19),
                new Rect(104, 143, 8, 23),
                new Rect(118, 148, 72, 13),
                new Rect(170, 175, 6, 11),
                new Rect(84, 177, 80, 7)
        );
        for (Rect crop : crops) {
            assertOpaque(image, crop.x, crop.y);
            assertOpaque(image, crop.right() - 1, crop.bottom() - 1);
            assertTransparent(image, crop.x - 1, crop.y - 1);
            assertTransparent(image, crop.right(), crop.bottom());
        }
    }

    private static void assertOpaque(BufferedImage image, int x, int y) {
        assertTrue((image.getRGB(x, y) >>> 24) > 0, "Expected authored pixels at " + x + "," + y);
    }

    private static void assertTransparent(BufferedImage image, int x, int y) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
        assertEquals(0, image.getRGB(x, y) >>> 24, "Crop touches a neighboring sprite at " + x + "," + y);
    }

    private record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
    }
}
