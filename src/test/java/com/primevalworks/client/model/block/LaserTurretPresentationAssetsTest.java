package com.primevalworks.client.model.block;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LaserTurretPresentationAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/primevalworks");

    @Test
    void authoredTurretModelAndFiringFramesStayTogether() throws Exception {
        String geometry = Files.readString(ASSETS.resolve("geckolib/models/block/laser_turret.geo.json"));
        String animation = Files.readString(ASSETS.resolve("geckolib/animations/block/laser_turret.animation.json"));
        assertTrue(geometry.contains("\"name\": \"base\"")
                        && geometry.contains("\"name\": \"head\"")
                        && geometry.contains("\"name\": \"mouth\""),
                "The authored turret pivot hierarchy is incomplete");
        assertTrue(animation.contains("\"firing\"") && animation.contains("\"mouth\""),
                "The turret firing animation no longer drives its jaw");

        BufferedImage idle = ImageIO.read(ASSETS.resolve("textures/block/laser_turret.png").toFile());
        BufferedImage firing = ImageIO.read(ASSETS.resolve("textures/block/laser_turret_firing.png").toFile());
        assertEquals(128, idle.getWidth());
        assertEquals(128, idle.getHeight());
        assertEquals(128, firing.getWidth());
        assertEquals(128 * 6, firing.getHeight(), "The six authored firing frames were not preserved");
    }

    @Test
    void spinosaurusTrophyAndEndgameRecipeRemainWired() throws Exception {
        String trophy = Files.readString(ASSETS.resolve("models/block/spinosaurus_head.json"));
        String turretItemModel = Files.readString(ASSETS.resolve("models/block/laser_turret_item.json"));
        String recipe = Files.readString(Path.of("src/main/resources/data/primevalworks/recipe/laser_turret.json"));
        String item = Files.readString(ASSETS.resolve("items/laser_turret.json"));
        assertEquals(21, trophy.split("\\\"from\\\"", -1).length - 1,
                "The trophy no longer contains the turret's complete head and mouth geometry");
        assertEquals(23, turretItemModel.split("\\\"from\\\"", -1).length - 1,
                "The normal block-item preview lost authored turret cubes");
        assertTrue(trophy.contains("primevalworks:block/laser_turret")
                        && turretItemModel.contains("primevalworks:block/laser_turret"),
                "A turret-derived block item stopped using the authored turret atlas");
        for (String ingredient : new String[] {
                "spinosaurus_head", "redstone_block", "compressed_ancient_metal_ingot",
                "laser_observer", "ancient_metal_ingot", "compressed_core"
        }) {
            assertTrue(recipe.contains(ingredient), "Laser Turret recipe lost: " + ingredient);
        }
        assertTrue(item.contains("minecraft:model")
                        && item.contains("primevalworks:block/laser_turret_item"),
                "The Laser Turret item is not using its normal vanilla block model");
    }
}
