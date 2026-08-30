package com.primevalworks.client.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThinPanelRenderContractTest {
    private static final Path SOURCE = Path.of("src/main/java/com/primevalworks/client/render");

    @Test
    void everyDinosaurUsesCutoutBackFaceCulling() throws Exception {
        String renderer = Files.readString(SOURCE.resolve("entity/FieldDodoRenderer.java"));
        assertTrue(renderer.contains("if (!renderState.isInvisible)"));
        assertTrue(renderer.contains("return RenderTypes.entityCutoutCull(texture);"));
        assertFalse(renderer.contains("profile.assetName().equals(\"pteranodon\")\n                 ||"),
                "Thin-panel culling was limited to a species allow-list again");
        assertFalse(renderer.contains("DinosaurMouthItemLayer"),
                "Transport cargo returned to the old mouth-mounted render layer");
        assertFalse(Files.exists(SOURCE.resolve("entity/DinosaurMouthItemLayer.java")),
                "The retired mouth-mounted cargo layer is still shipped");
    }

    @Test
    void animatedBlocksChooseTheCutoutModeTheirGeometryNeeds() throws Exception {
        String turbine = Files.readString(SOURCE.resolve("block/TurbineRenderer.java"));
        assertTrue(turbine.contains("TurbineGeoModel.WIND")
                        && turbine.contains("RenderTypes.entityCutoutCull(texture)")
                        && turbine.contains("RenderTypes.entityCutout(texture)"),
                "Wind panels must cull overlapping zero-depth faces without changing the solid water rotor");
        String turbineItem = Files.readString(SOURCE.resolve("item/TurbineItemRenderer.java"));
        assertTrue(turbineItem.contains("variant == TurbineBlockItem.Variant.WATER")
                        && turbineItem.contains("RenderTypes.entityCutoutCull(texture)"),
                "Wind turbine items lost the same zero-depth face treatment as Pteranodon wings");
        String turret = Files.readString(SOURCE.resolve("block/LaserTurretRenderer.java"));
        assertTrue(turret.contains("RenderTypes.entityCutoutCull(texture)"),
                "Solid turret geometry lost its normal back-face culling");
    }
}
