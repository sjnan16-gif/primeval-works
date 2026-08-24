package com.primevalworks.client.model.entity;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ParasaurolophusPresentationAssetsTest {
    @Test
    void exportedModelTextureAndRequiredAnimationsStayTogether() throws Exception {
        Path root = Path.of("src/main/resources/assets/primevalworks");
        Path geo = root.resolve("geckolib/models/entity/parasaurolophus.geo.json");
        Path animations = root.resolve("geckolib/animations/entity/parasaurolophus.animation.json");
        Path texture = root.resolve("textures/entity/parasaurolophus.png");
        Path blink = root.resolve("textures/entity/parasaurolophus_blink.png");
        assertTrue(Files.isRegularFile(geo), "Missing Parasaurolophus geometry export");
        assertTrue(Files.isRegularFile(animations), "Missing Parasaurolophus animation export");
        assertTrue(Files.isRegularFile(texture), "Missing Parasaurolophus texture");
        assertTrue(Files.isRegularFile(blink), "Missing Parasaurolophus blink texture");

        String animationJson = Files.readString(animations);
        for (String name : new String[]{"idle", "walk", "run", "work", "sleep"}) {
            assertTrue(animationJson.contains("\"" + name + "\""),
                    "Parasaurolophus export is missing animation: " + name);
        }
        var baseImage = ImageIO.read(texture.toFile());
        var blinkImage = ImageIO.read(blink.toFile());
        assertTrue(baseImage != null && baseImage.getWidth() == 256 && baseImage.getHeight() == 256,
                "Parasaurolophus base texture must remain 256x256");
        assertTrue(blinkImage != null && blinkImage.getWidth() == 256 && blinkImage.getHeight() == 256,
                "Parasaurolophus blink texture must remain 256x256");
    }
}
