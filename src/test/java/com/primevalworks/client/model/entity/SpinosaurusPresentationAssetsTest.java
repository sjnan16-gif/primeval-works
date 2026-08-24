package com.primevalworks.client.model.entity;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpinosaurusPresentationAssetsTest {
    @Test
    void authoredSpinosaurusExportKeepsEveryUsedAnimation() throws Exception {
        Path root = Path.of("src/main/resources/assets/primevalworks");
        Path geo = root.resolve("geckolib/models/entity/spino.geo.json");
        Path animations = root.resolve("geckolib/animations/entity/spino.animation.json");
        Path texture = root.resolve("textures/entity/spino.png");
        assertTrue(Files.isRegularFile(geo), "Missing Spinosaurus geometry export");
        assertTrue(Files.isRegularFile(animations), "Missing Spinosaurus animation export");
        assertTrue(Files.isRegularFile(texture), "Missing Spinosaurus texture");

        String animationJson = Files.readString(animations);
        String geometryJson = Files.readString(geo);
        for (String name : new String[]{"idle", "walk", "work", "sleep", "swim", "attack"}) {
            assertTrue(animationJson.contains("\"" + name + "\""),
                    "Spinosaurus export is missing animation: " + name);
        }
        assertTrue(geometryJson.contains("\"name\": \"whereplayersits\""),
                "Spinosaurus export is missing its authored rider locator");
        assertTrue(geometryJson.contains("\"pivot\": [0, 68.5, -18.5]"),
                "Spinosaurus rider locator moved without updating the ride contract");
        assertTrue(animationJson.contains("\"whereplayersits\""),
                "Spinosaurus swim animation no longer carries the rider locator");
        var image = ImageIO.read(texture.toFile());
        assertTrue(image != null && image.getWidth() == 512 && image.getHeight() == 512,
                "Spinosaurus texture must match the authored 512x512 state atlases");
        for (String state : new String[]{"spino_blink.png", "spino_saddled.png",
                "spino_saddled_blink.png", "spino_saddled_aquatic.png",
                "spino_saddled_aquatic_blink.png"}) {
            var stateImage = ImageIO.read(root.resolve("textures/entity/" + state).toFile());
            assertTrue(stateImage != null && stateImage.getWidth() == 512 && stateImage.getHeight() == 512,
                    "Spinosaurus state texture must match the base UV atlas: " + state);
        }
    }
}
