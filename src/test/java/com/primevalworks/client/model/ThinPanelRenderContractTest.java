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
    }

    @Test
    void animatedBlocksUseCutoutBackFaceCulling() throws Exception {
        for (String rendererName : new String[] {"TurbineRenderer.java", "LaserTurretRenderer.java"}) {
            String renderer = Files.readString(SOURCE.resolve("block/" + rendererName));
            assertTrue(renderer.contains("RenderTypes.entityCutoutCull(texture)"),
                    rendererName + " can blend coincident front/back panel textures");
        }
    }
}
