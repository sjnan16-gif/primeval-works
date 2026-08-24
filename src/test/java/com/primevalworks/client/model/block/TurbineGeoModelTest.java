package com.primevalworks.client.model.block;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TurbineGeoModelTest {
    @Test
    void animationIdentifiersUseGeckoLibFiveCanonicalPaths() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/primevalworks/client/model/block/TurbineGeoModel.java"));
        assertTrue(source.contains("asset(\"block/wind_turbine\")"));
        assertTrue(source.contains("asset(\"block/water_turbine\")"));
        assertTrue(!source.contains("asset(\"block/wind_turbine.animation\")"));
        assertTrue(!source.contains("asset(\"block/water_turbine.animation\")"));
        assertTrue(!source.contains("geckolib/models/block/wind_turbine"));
        assertTrue(!source.contains("geckolib/models/block/water_turbine"));
        assertTrue(!source.contains("geckolib/animations/block/wind_turbine"));
        assertTrue(!source.contains("geckolib/animations/block/water_turbine"));
    }

    @Test
    void everyTurbineAnimationContainsTheRequestedSpinClip() throws IOException {
        assertSpinClip("wind_turbine");
        assertSpinClip("water_turbine");
    }

    @Test
    void turbineTexturesMatchTheirExportedUvCanvas() throws IOException {
        assertTextureCanvas("wind_turbine", 64, 64);
        assertTextureCanvas("water_turbine", 64, 64);
    }

    @Test
    void turbineGeometryIsCenteredAndWaterWheelIsTwoBlocksWide() throws IOException {
        String wind = Files.readString(Path.of(
                "src/main/resources/assets/primevalworks/geckolib/models/block/wind_turbine.geo.json"));
        String water = Files.readString(Path.of(
                "src/main/resources/assets/primevalworks/geckolib/models/block/water_turbine.geo.json"));
        assertTrue(wind.contains("\"visible_bounds_offset\": [0, 1.75, 0]"));
        assertTrue(water.contains("\"origin\": [-16, 0, -6], \"size\": [32, 3, 12]"));
        assertTrue(water.contains("\"visible_bounds_offset\": [0, 1, 0]"));
        assertTrue(!water.contains("\"size\": [64,"), "Water turbine regressed to four blocks wide");
    }

    private static void assertSpinClip(String name) throws IOException {
        Path resource = Path.of("src/main/resources/assets/primevalworks/geckolib/animations/block/"
                + name + ".animation.json");
        assertTrue(Files.isRegularFile(resource), "Missing GeckoLib animation resource: " + resource);
        String json = Files.readString(resource);
        assertTrue(json.contains("\"spin\""), "Animation resource has no spin clip: " + resource);
        assertTrue(json.contains("\"animation_length\""),
                "Animation resource has no usable timeline: " + resource);
    }

    private static void assertTextureCanvas(String name, int width, int height) throws IOException {
        Path resource = Path.of("src/main/resources/assets/primevalworks/textures/block/" + name + ".png");
        assertTrue(Files.isRegularFile(resource), "Missing turbine texture: " + resource);
        BufferedImage image = ImageIO.read(resource.toFile());
        assertTrue(image != null, "Unreadable turbine texture: " + resource);
        assertTrue(image.getWidth() == width && image.getHeight() == height,
                "Wrong turbine UV canvas: " + resource);
    }
}
