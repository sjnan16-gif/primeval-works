package com.primevalworks.client.model.entity;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SaddleItemAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/primevalworks");
    @Test
    void bothMountSaddlesUseTheirAuthoredLocalSprites() throws Exception {
        for (String saddle : new String[]{"pteranodon_saddle", "spinosaurus_saddle"}) {
            Path texture = ASSETS.resolve("textures/item/" + saddle + ".png");
            var image = ImageIO.read(texture.toFile());
            assertEquals(16, image.getWidth(), saddle);
            assertEquals(16, image.getHeight(), saddle);

            String item = Files.readString(ASSETS.resolve("items/" + saddle + ".json"));
            String model = Files.readString(ASSETS.resolve("models/item/" + saddle + ".json"));
            assertTrue(item.contains("primevalworks:item/" + saddle), saddle);
            assertTrue(model.contains("primevalworks:item/" + saddle), saddle);
        }
    }
}
