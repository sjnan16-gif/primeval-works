package com.primevalworks.client.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockPreviewAssetsTest {
    private static final Pattern MODEL_REFERENCE = Pattern.compile("\\\"model\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final List<String> BLOCK_ITEMS = List.of(
            "command_table", "food_box", "wind_turbine", "water_turbine",
            "reinforced_piston", "sticky_reinforced_piston", "laser_observer", "ancient_barrel",
            "dart_turret", "processor", "ancient_furnace",
            "ancient_spell_stone", "laser_turret", "premium_egg_incubator", "enhanced_rail",
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
            }
        }
    }
}
