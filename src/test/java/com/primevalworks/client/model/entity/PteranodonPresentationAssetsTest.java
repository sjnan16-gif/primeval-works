package com.primevalworks.client.model.entity;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PteranodonPresentationAssetsTest {
    @Test
    void saddledAndSleepingPresentationAssetsUseExpectedCanvases() throws IOException {
        assertCanvas("textures/entity/pteranodon_saddled.png", 128, 128);
        assertCanvas("textures/entity/pteranodon_saddled_blink.png", 128, 128);
        assertCanvas("textures/entity/indicator/z.png", 16, 16);
    }

    private static void assertCanvas(String relativePath, int width, int height) throws IOException {
        Path resource = Path.of("src/main/resources/assets/primevalworks").resolve(relativePath);
        assertTrue(Files.isRegularFile(resource), "Missing presentation asset: " + resource);
        BufferedImage image = ImageIO.read(resource.toFile());
        assertTrue(image != null, "Unreadable presentation asset: " + resource);
        assertTrue(image.getWidth() == width && image.getHeight() == height,
                "Wrong presentation asset canvas: " + resource);
    }
}
