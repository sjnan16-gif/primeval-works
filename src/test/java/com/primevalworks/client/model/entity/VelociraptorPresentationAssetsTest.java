package com.primevalworks.client.model.entity;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VelociraptorPresentationAssetsTest {
    @Test
    void authoredVelociraptorExportKeepsEveryRuntimeAssetTogether() throws Exception {
        Path root = Path.of("src/main/resources/assets/primevalworks");
        Path model = root.resolve("geckolib/models/entity/velociraptor.geo.json");
        Path animations = root.resolve("geckolib/animations/entity/velociraptor.animation.json");
        Path texture = root.resolve("textures/entity/velociraptor.png");
        Path blink = root.resolve("textures/entity/velociraptor_blink.png");

        assertTrue(Files.isRegularFile(model), "Missing Velociraptor GeckoLib model export");
        assertTrue(Files.isRegularFile(animations), "Missing Velociraptor GeckoLib animation export");
        assertTrue(Files.isRegularFile(texture), "Missing Velociraptor texture");
        assertTrue(Files.isRegularFile(blink), "Missing Velociraptor blink texture");

        String modelJson = Files.readString(model);
        for (String bone : new String[]{"body", "head2", "lowerjaw", "tail", "rightleg", "leftleg"}) {
            assertTrue(modelJson.contains("\"name\": \"" + bone + "\""),
                    "Velociraptor export is missing runtime bone: " + bone);
        }

        String animationJson = Files.readString(animations);
        for (String clip : new String[]{"idle", "walk", "run", "work", "sleep", "attack"}) {
            assertTrue(animationJson.contains("\"" + clip + "\""),
                    "Velociraptor export is missing animation: " + clip);
        }

        var openImage = ImageIO.read(texture.toFile());
        var blinkImage = ImageIO.read(blink.toFile());
        assertEquals(128, openImage.getWidth());
        assertEquals(128, openImage.getHeight());
        assertEquals(openImage.getWidth(), blinkImage.getWidth());
        assertEquals(openImage.getHeight(), blinkImage.getHeight());
    }
}
