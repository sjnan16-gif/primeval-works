package com.primevalworks.client.model.block;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AncientBarrelPresentationAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/primevalworks");
    private static final Pattern UV = Pattern.compile("\\\"uv\\\"\\s*:\\s*\\[([^]]+)]");

    @Test
    void authoredClosedAndOpenBarrelStatesStayWired() throws Exception {
        BufferedImage closed = ImageIO.read(ASSETS.resolve("textures/block/ancient_barrel.png").toFile());
        BufferedImage open = ImageIO.read(ASSETS.resolve("textures/block/ancient_barrel_open.png").toFile());
        assertEquals(64, closed.getWidth());
        assertEquals(64, closed.getHeight());
        assertEquals(64, open.getWidth());
        assertEquals(64, open.getHeight());

        String states = Files.readString(ASSETS.resolve("blockstates/ancient_barrel.json"));
        String closedModel = Files.readString(ASSETS.resolve("models/block/ancient_barrel.json"));
        String openModel = Files.readString(ASSETS.resolve("models/block/ancient_barrel_open.json"));
        assertTrue(states.contains("open=false") && states.contains("open=true"));
        assertTrue(closedModel.contains("primevalworks:block/ancient_barrel"));
        assertTrue(openModel.contains("primevalworks:block/ancient_barrel_open"));
        assertNormalizedUvs(closedModel);
        assertNormalizedUvs(openModel);
    }

    private static void assertNormalizedUvs(String model) {
        Matcher matcher = UV.matcher(model);
        int faces = 0;
        while (matcher.find()) {
            faces++;
            for (String coordinate : matcher.group(1).split(",")) {
                double value = Double.parseDouble(coordinate.trim());
                assertTrue(value >= 0.0 && value <= 16.0,
                        "Vanilla block-model UVs must stay normalized to 0-16: " + value);
            }
        }
        assertEquals(6, faces);
    }
}
