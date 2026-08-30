package com.primevalworks.client.model.block;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProcessorModelAssetsTest {
    @Test
    void authoredProcessorModelAndTextureArePresent() throws Exception {
        Path model = Path.of(
                "src/main/resources/assets/primevalworks/models/block/processor.json");
        Path texture = Path.of(
                "src/main/resources/assets/primevalworks/textures/block/processor.png");
        assertTrue(Files.isRegularFile(model));
        assertTrue(Files.isRegularFile(texture));
        String modelJson = Files.readString(model);
        assertTrue(modelJson.contains("primevalworks:block/processor"));
        assertTrue(modelJson.contains("\"uv\": [12, 4, 16, 8]"));
        BufferedImage image = ImageIO.read(texture.toFile());
        assertNotNull(image);
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
    }
}
