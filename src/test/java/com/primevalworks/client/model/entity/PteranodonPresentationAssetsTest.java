package com.primevalworks.client.model.entity;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class PteranodonPresentationAssetsTest {
    @Test
    void saddledAndSleepingPresentationAssetsUseExpectedCanvases() throws IOException {
        assertCanvas("textures/entity/pteranodon_saddled.png", 128, 128);
        assertCanvas("textures/entity/pteranodon_saddled_blink.png", 128, 128);
        assertCanvas("textures/entity/indicator/z.png", 16, 16);
        assertMatchingBlinkPixels(
                "textures/entity/pteranodon.png",
                "textures/entity/pteranodon_blink.png",
                "textures/entity/pteranodon_saddled.png",
                "textures/entity/pteranodon_saddled_blink.png"
        );
    }

    private static void assertCanvas(String relativePath, int width, int height) throws IOException {
        Path resource = Path.of("src/main/resources/assets/primevalworks").resolve(relativePath);
        assertTrue(Files.isRegularFile(resource), "Missing presentation asset: " + resource);
        BufferedImage image = ImageIO.read(resource.toFile());
        assertTrue(image != null, "Unreadable presentation asset: " + resource);
        assertTrue(image.getWidth() == width && image.getHeight() == height,
                "Wrong presentation asset canvas: " + resource);
    }

    private static void assertMatchingBlinkPixels(
            String plainOpen,
            String plainBlink,
            String saddledOpen,
            String saddledBlink
    ) throws IOException {
        BufferedImage open = read(plainOpen);
        BufferedImage blink = read(plainBlink);
        BufferedImage saddle = read(saddledOpen);
        BufferedImage saddleBlink = read(saddledBlink);
        int changed = 0;
        for (int y = 0; y < open.getHeight(); y++) {
            for (int x = 0; x < open.getWidth(); x++) {
                boolean plainChanged = open.getRGB(x, y) != blink.getRGB(x, y);
                assertEquals(plainChanged, saddle.getRGB(x, y) != saddleBlink.getRGB(x, y),
                        "Pteranodon saddle lost a blink pixel at " + x + "," + y);
                if (plainChanged) changed++;
            }
        }
        assertTrue(changed > 0, "Pteranodon open and blink atlases are identical");
    }

    private static BufferedImage read(String relativePath) throws IOException {
        return ImageIO.read(Path.of("src/main/resources/assets/primevalworks").resolve(relativePath).toFile());
    }
}
