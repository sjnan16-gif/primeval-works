package com.primevalworks.client.model;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuthoredProgressionAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/primevalworks");
    private static final Path DATA = Path.of("src/main/resources/data/primevalworks");

    @Test
    void magicShardChainUsesCanonicalItemsAndEveryAuthoredFrame() throws Exception {
        assertImage("textures/item/magic_shard_fragment.png", 16, 16);
        assertImage("textures/item/magic_shard.png", 16, 96);
        assertLocalItemModel("magic_shard_fragment");
        assertLocalItemModel("magic_shard");

        String animation = Files.readString(ASSETS.resolve("textures/item/magic_shard.png.mcmeta"));
        assertTrue(animation.contains("[0,1,2,3,4,5]"));

        String registry = Files.readString(Path.of(
                "src/main/java/com/primevalworks/registry/ModItems.java"));
        assertTrue(registry.contains("registerSimpleItem(\"magic_shard_fragment\")"));
        assertTrue(registry.contains("registerSimpleItem(\"magic_shard\")"));
        assertTrue(registry.contains("\"raw_ancient_spell_ingot\"")
                && registry.contains("\"ancient_spell_ingot\""));
        assertFalse(registry.contains("registerSimpleItem(\"raw_ancient_spell_ingot\")"));
        assertFalse(registry.contains("registerSimpleItem(\"ancient_spell_ingot\")"));

        String processor = Files.readString(Path.of(
                "src/main/java/com/primevalworks/world/processor/ProcessorRecipes.java"));
        assertTrue(processor.contains("ModItems.MAGIC_SHARD_FRAGMENT.get()"));
        assertTrue(processor.contains("ModItems.MAGIC_SHARD.get()"));
        for (String recipe : new String[] {
                "ancient_spell_stone.json", "premium_egg_incubator.json", "primordial_sword.json"
        }) {
            assertTrue(Files.readString(DATA.resolve("recipe/" + recipe)).contains("primevalworks:magic_shard"));
        }
    }

    @Test
    void ancientMetalArtAndNuggetConversionAreComplete() throws Exception {
        assertImage("textures/item/ancient_metal_ingot.png", 16, 16);
        assertImage("textures/item/ancient_metal_nugget.png", 16, 16);
        assertLocalItemModel("ancient_metal_ingot");
        assertLocalItemModel("ancient_metal_nugget");
        assertTrue(Files.readString(DATA.resolve("recipe/ancient_metal_nugget_from_ingot.json"))
                .contains("\"count\": 9"));
        assertTrue(Files.readString(DATA.resolve("recipe/ancient_metal_ingot_from_nuggets.json"))
                .contains("primevalworks:ancient_metal_nugget"));
    }

    @Test
    void laserObserverUsesBothAuthoredStatesAndExactCubeAtlasUvs() throws Exception {
        assertImage("textures/block/laser_observer.png", 64, 64);
        assertImage("textures/block/laser_observer_powered.png", 64, 64);
        String model = Files.readString(ASSETS.resolve("models/block/laser_observer.json"));
        assertTrue(model.contains("\"uv\": [4, 4, 8, 8]"));
        assertTrue(model.contains("\"uv\": [12, 4, 16, 8]"));
        String powered = Files.readString(ASSETS.resolve("models/block/laser_observer_powered.json"));
        assertTrue(powered.contains("primevalworks:block/laser_observer_powered"));
        String states = Files.readString(ASSETS.resolve("blockstates/laser_observer.json"));
        assertTrue(states.contains("facing=down,powered=true"));
        assertTrue(states.contains("facing=up,powered=false"));
        assertTrue(states.contains("facing=west,powered=true"));
    }

    private static void assertLocalItemModel(String name) throws Exception {
        assertTrue(Files.readString(ASSETS.resolve("items/" + name + ".json"))
                .contains("primevalworks:item/" + name));
        assertTrue(Files.readString(ASSETS.resolve("models/item/" + name + ".json"))
                .contains("primevalworks:item/" + name));
    }

    private static void assertImage(String relative, int width, int height) throws Exception {
        BufferedImage image = ImageIO.read(ASSETS.resolve(relative).toFile());
        assertEquals(width, image.getWidth(), relative);
        assertEquals(height, image.getHeight(), relative);
    }
}
