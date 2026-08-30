package com.primevalworks.client.model;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FoodBoxPresentationAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/primevalworks");

    @Test
    void authoredEmptyAndFullModelsStayWiredToInventoryState() throws Exception {
        assertImage("textures/block/food_box_empty.png", 128, 128);
        assertImage("textures/block/food_box_full.png", 64, 64);

        String states = Files.readString(ASSETS.resolve("blockstates/food_box.json"));
        assertTrue(states.contains("\"full=false\"") && states.contains("primevalworks:block/food_box"));
        assertTrue(states.contains("\"full=true\"") && states.contains("primevalworks:block/food_box_full"));

        String empty = Files.readString(ASSETS.resolve("models/block/food_box.json"));
        assertTrue(empty.contains("primevalworks:block/food_box_empty"));
        assertTrue(empty.contains("\"from\": [2, 13, 14]")
                        && empty.contains("\"to\": [14, 16, 14]"),
                "The updated empty Food Box rim was replaced by the old lowered face");
        assertEquals(6, countOccurrences(empty, "\"from\""),
                "The empty Food Box lost part of its authored six-element geometry");

        String full = Files.readString(ASSETS.resolve("models/block/food_box_full.json"));
        assertTrue(full.contains("primevalworks:block/food_box_full"));
        assertEquals(1, countOccurrences(full, "\"from\""),
                "The full Food Box no longer uses its authored single-cube geometry");

    }

    @Test
    void authoredWhistleSpriteReplacesTheVanillaPlaceholder() throws Exception {
        assertImage("textures/item/dino_whistle.png", 16, 16);
        String model = Files.readString(ASSETS.resolve("models/item/dino_whistle.json"));
        assertTrue(model.contains("primevalworks:item/dino_whistle"));
    }

    private static int countOccurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }

    private static void assertImage(String relative, int width, int height) throws Exception {
        BufferedImage image = ImageIO.read(ASSETS.resolve(relative).toFile());
        assertEquals(width, image.getWidth(), relative);
        assertEquals(height, image.getHeight(), relative);
    }
}
