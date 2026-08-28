package com.primevalworks.client.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class BlockPreviewAssetsTest {
    private static final Pattern MODEL_REFERENCE = Pattern.compile("\\\"(?:model|base)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern PARENT_REFERENCE = Pattern.compile("\\\"parent\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Set<String> FITTED_PRESENTATION_TEMPLATES = Set.of(
            "template_compact_block.json", "template_dinosaur_egg.json");
    private static final List<String> BLOCK_ITEMS = List.of(
            "command_table", "food_box", "wind_turbine", "upgraded_wind_turbine", "water_turbine",
            "laser_observer", "ancient_barrel",
            "dart_turret", "processor", "ancient_furnace",
            "ancient_spell_stone", "laser_turret", "spinosaurus_head", "premium_egg_incubator",
            "small_dinosaur_egg", "big_dinosaur_egg", "large_dinosaur_egg"
    );
    private static final Map<String, String> EXPECTED_PREVIEW_MODELS = Map.ofEntries(
            Map.entry("command_table", "primevalworks:block/command_table"),
            Map.entry("food_box", "primevalworks:block/food_box"),
            Map.entry("wind_turbine", "primevalworks:item/wind_turbine"),
            Map.entry("upgraded_wind_turbine", "primevalworks:item/upgraded_wind_turbine"),
            Map.entry("water_turbine", "primevalworks:item/water_turbine"),
            Map.entry("laser_observer", "primevalworks:block/laser_observer"),
            Map.entry("ancient_barrel", "primevalworks:block/ancient_barrel"),
            Map.entry("dart_turret", "primevalworks:block/dart_turret_item"),
            Map.entry("processor", "primevalworks:block/processor"),
            Map.entry("ancient_furnace", "primevalworks:block/ancient_furnace"),
            Map.entry("ancient_spell_stone", "primevalworks:block/ancient_spell_stone"),
            Map.entry("laser_turret", "primevalworks:block/laser_turret_item"),
            Map.entry("spinosaurus_head", "primevalworks:block/spinosaurus_head"),
            Map.entry("premium_egg_incubator", "primevalworks:block/premium_egg_incubator"),
            Map.entry("small_dinosaur_egg", "primevalworks:block/small_dinosaur_egg"),
            Map.entry("big_dinosaur_egg", "primevalworks:block/big_dinosaur_egg"),
            Map.entry("large_dinosaur_egg", "primevalworks:block/large_dinosaur_egg")
    );

    @Test
    void everyPlaceableBlockHasAnInventoryPreviewModel() throws Exception {
        for (String name : BLOCK_ITEMS) {
            Path definitionPath = Path.of("src/main/resources/assets/primevalworks/items/" + name + ".json");
            assertTrue(Files.isRegularFile(definitionPath), "Missing block item definition: " + definitionPath);
            Matcher matcher = MODEL_REFERENCE.matcher(Files.readString(definitionPath));
            assertTrue(matcher.find(), "Block item has no preview model: " + name);
            String model = matcher.group(1);
            assertTrue(model.equals(EXPECTED_PREVIEW_MODELS.get(name)),
                    "Block item points at the wrong preview model: " + name + " -> " + model);
            if (model.startsWith("primevalworks:")) {
                Path localModel = Path.of("src/main/resources/assets/primevalworks/models/"
                        + model.substring("primevalworks:".length()) + ".json");
                assertTrue(Files.isRegularFile(localModel), "Missing local block preview model: " + localModel);
                assertUsesMinecraftPresentation(localModel, new HashSet<>());
            } else {
                assertTrue(model.startsWith("minecraft:block/"),
                        "Block item does not use a normal block presentation: " + name + " -> " + model);
            }
        }
    }

    @Test
    void reviewedNonCubeProfilesStaySeparateFromPlacedGeometry() throws Exception {
        String compact = Files.readString(Path.of(
                "src/main/resources/assets/primevalworks/models/item/template_compact_block.json"));
        assertTrue(compact.contains("\"scale\": [1, 1, 1]"),
                "Compact trophies and turrets became tiny in inventory previews");
        assertTrue(compact.contains("\"scale\": [0.6, 0.6, 0.6]"),
                "Compact trophies and turrets no longer use a hand-safe fitted scale");

        assertParent("laser_turret_item", "primevalworks:item/template_compact_block");
        assertParent("spinosaurus_head", "primevalworks:item/template_compact_block");
        for (String egg : List.of("small_dinosaur_egg", "big_dinosaur_egg", "large_dinosaur_egg")) {
            assertParent(egg, "primevalworks:item/template_dinosaur_egg");
        }
    }

    private static void assertParent(String blockModel, String expectedParent) throws Exception {
        String json = Files.readString(Path.of(
                "src/main/resources/assets/primevalworks/models/block/" + blockModel + ".json"));
        assertTrue(json.contains("\"parent\": \"" + expectedParent + "\""),
                blockModel + " stopped using its reviewed item-only presentation");
    }

    private static void assertUsesMinecraftPresentation(Path model, Set<Path> visited) throws Exception {
        Path normalized = model.normalize();
        assertTrue(visited.add(normalized), "Circular block-item model parent: " + normalized);
        String json = Files.readString(normalized);
        if (json.contains("\"display\"")) {
            assertTrue(FITTED_PRESENTATION_TEMPLATES.contains(normalized.getFileName().toString()),
                    "A block item bypasses the reviewed non-cube presentation templates: " + normalized);
        }
        Matcher parent = PARENT_REFERENCE.matcher(json);
        assertTrue(parent.find(), "Block-item model has no block-model parent: " + normalized);
        String parentId = parent.group(1);
        if (parentId.startsWith("minecraft:block/")) return;
        if (parentId.startsWith("primevalworks:")) {
            Path parentModel = Path.of("src/main/resources/assets/primevalworks/models/"
                    + parentId.substring("primevalworks:".length()) + ".json");
            assertTrue(Files.isRegularFile(parentModel), "Missing local model parent: " + parentModel);
            assertUsesMinecraftPresentation(parentModel, visited);
            return;
        }
        fail("Block-item model leaves the vanilla block presentation chain: " + normalized + " -> " + parentId);
    }
}
