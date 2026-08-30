package com.primevalworks.client.model.entity;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TriceratopsPresentationAssetsTest {
    @Test
    void authoredTriceratopsUsesItsCompleteSuppliedPresentation() throws Exception {
        Path root = Path.of("src/main/resources/assets/primevalworks");
        Path model = root.resolve("geckolib/models/entity/triceratops.geo.json");
        Path animations = root.resolve("geckolib/animations/entity/triceratops.animation.json");
        Path texture = root.resolve("textures/entity/triceratops.png");
        Path blink = root.resolve("textures/entity/triceratops_blink.png");

        assertTrue(Files.isRegularFile(model));
        assertTrue(Files.isRegularFile(animations));
        assertTrue(Files.isRegularFile(texture));
        assertTrue(Files.isRegularFile(blink));

        String modelJson = Files.readString(model);
        for (String bone : new String[]{
                "body", "head", "lowerjaw", "crown", "tail", "segment2",
                "frontrightleg", "frontleftleg", "backrightleg", "backleftleg"
        }) {
            assertTrue(modelJson.contains("\"name\": \"" + bone + "\""),
                    "Triceratops export is missing runtime bone: " + bone);
        }

        String animationJson = Files.readString(animations);
        for (String clip : new String[]{"Idle", "sleep", "walk", "work"}) {
            assertTrue(animationJson.contains("\"" + clip + "\": {"),
                    "Triceratops export is missing supplied animation: " + clip);
        }
        assertFalse(animationJson.contains("\"run\": {"));
        assertFalse(animationJson.contains("\"attack\": {"));

        var openImage = ImageIO.read(texture.toFile());
        var blinkImage = ImageIO.read(blink.toFile());
        assertEquals(256, openImage.getWidth());
        assertEquals(256, openImage.getHeight());
        assertEquals(openImage.getWidth(), blinkImage.getWidth());
        assertEquals(openImage.getHeight(), blinkImage.getHeight());

        DinosaurPreviewBounds bounds = DinosaurPreviewBounds.forAsset("triceratops", 1.0F);
        assertEquals(2.25F, bounds.width());
        assertEquals(3.375F, bounds.height());
        assertEquals(7.0F, bounds.depth());
    }
}
