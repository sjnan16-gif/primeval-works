package com.primevalworks.client.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class BlockPreviewAssetsTest {
    private static final Pattern MODEL_REFERENCE = Pattern.compile("\\\"(?:model|base)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern PARENT_REFERENCE = Pattern.compile("\\\"parent\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final List<String> BLOCK_ITEMS = List.of(
            "command_table", "food_box", "wind_turbine", "upgraded_wind_turbine", "water_turbine",
            "reinforced_piston", "sticky_reinforced_piston", "laser_observer", "ancient_barrel",
            "dart_turret", "processor", "ancient_furnace",
            "ancient_spell_stone", "laser_turret", "spinosaurus_head", "premium_egg_incubator",
            "small_dinosaur_egg", "big_dinosaur_egg", "large_dinosaur_egg"
    );

    @Test
    void everyPlaceableBlockHasAnInventoryPreviewModel() throws Exception {
        for (String name : BLOCK_ITEMS) {
            Path definitionPath = Path.of("src/main/resources/assets/primevalworks/items/" + name + ".json");
            assertTrue(Files.isRegularFile(definitionPath), "Missing block item definition: " + definitionPath);
            Matcher matcher = MODEL_REFERENCE.matcher(Files.readString(definitionPath));
            assertTrue(matcher.find(), "Block item has no preview model: " + name);
            String model = matcher.group(1);
            if (model.startsWith("primevalworks:")) {
                Path localModel = Path.of("src/main/resources/assets/primevalworks/models/"
                        + model.substring("primevalworks:".length()) + ".json");
                assertTrue(Files.isRegularFile(localModel), "Missing local block preview model: " + localModel);
                assertUsesVanillaBlockPresentation(localModel, new HashSet<>());
            } else {
                assertTrue(model.startsWith("minecraft:block/"),
                        "Block item does not use a normal block presentation: " + name + " -> " + model);
            }
        }
    }

    private static void assertUsesVanillaBlockPresentation(Path model, Set<Path> visited) throws Exception {
        Path normalized = model.normalize();
        assertTrue(visited.add(normalized), "Circular block-item model parent: " + normalized);
        String json = Files.readString(normalized);
        assertFalse(json.contains("\"display\""),
                "Block-item model overrides Minecraft's standard camera transforms: " + normalized);
        Matcher parent = PARENT_REFERENCE.matcher(json);
        assertTrue(parent.find(), "Block-item model has no block-model parent: " + normalized);
        String parentId = parent.group(1);
        if (parentId.startsWith("minecraft:block/")) return;
        if (parentId.startsWith("primevalworks:")) {
            Path parentModel = Path.of("src/main/resources/assets/primevalworks/models/"
                    + parentId.substring("primevalworks:".length()) + ".json");
            assertTrue(Files.isRegularFile(parentModel), "Missing local model parent: " + parentModel);
            assertUsesVanillaBlockPresentation(parentModel, visited);
            return;
        }
        fail("Block-item model leaves the vanilla block presentation chain: " + normalized + " -> " + parentId);
    }
}
