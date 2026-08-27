package com.primevalworks.client.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TurbineItemPresentationAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/primevalworks");

    @Test
    void turbineItemsUseTheirAuthoredGeckoModels() throws Exception {
        assertTurbineItem("wind_turbine", "wind_turbine");
        assertTurbineItem("upgraded_wind_turbine", "wind_turbine");
        assertTurbineItem("water_turbine", "water_turbine");

        String renderer = Files.readString(Path.of(
                "src/main/java/com/primevalworks/client/render/item/TurbineItemRenderer.java"));
        assertTrue(renderer.contains("RenderTypes.entityCutoutCull(texture)"),
                "Turbine item planes can blend their front and rear textures");
        assertTrue(renderer.contains("case GUI -> 0.48F")
                        && renderer.contains("FIRST_PERSON_RIGHT_HAND -> 0.33F")
                        && renderer.contains("THIRD_PERSON_RIGHT_HAND -> 0.45F"),
                "The authored multiblock turbine stopped fitting both its slot and held views");
        assertTrue(renderer.contains("context == ItemDisplayContext.GUI")
                        && renderer.contains("variant.guiOffsetX()")
                        && renderer.contains("variant.guiOffsetY()")
                        && renderer.contains("variant.guiOffsetZ()"),
                "The turbines are no longer centered from their visible GUI bounds");

        String item = Files.readString(Path.of(
                "src/main/java/com/primevalworks/world/item/TurbineBlockItem.java"));
        assertTrue(item.contains("1.28F, -0.18F")
                        && item.contains("1.35F, -0.90F"),
                "A turbine lost its individually verified 16x16 GUI center");
        assertTrue(item.contains("WATER(\"water_turbine\", \"water_turbine\", \"Water Turbine\", 0.64F"),
                "The larger authored water rotor no longer fits the item slot and held views");
        assertTrue(item.contains("Component getName(ItemStack stack)"),
                "Turbine stacks no longer provide their explicit display names");
    }

    @Test
    void obsoletePrimitiveTurbinePlaceholdersAreGone() throws Exception {
        for (String name : new String[] {"wind_turbine", "water_turbine"}) {
            String base = Files.readString(ASSETS.resolve("models/item/" + name + ".json"));
            assertFalse(base.contains("\"elements\""),
                    name + " still renders an old hand-built placeholder instead of the authored model");
        }
    }

    private static void assertTurbineItem(String item, String authoredModel) throws Exception {
        String definition = Files.readString(ASSETS.resolve("items/" + item + ".json"));
        assertTrue(definition.contains("\"type\": \"minecraft:special\""));
        assertTrue(definition.contains("\"type\": \"geckolib:geckolib\""));
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "geckolib/models/block/" + authoredModel + ".geo.json")));
    }
}
