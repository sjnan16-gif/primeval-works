package com.primevalworks.client.model.block;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MagicTurretPresentationAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/primevalworks");

    @Test
    void authoredTurretModelAndFiringFramesStayTogether() throws Exception {
        String geometry = Files.readString(ASSETS.resolve("geckolib/models/block/magic_turret.geo.json"));
        String animation = Files.readString(ASSETS.resolve("geckolib/animations/block/magic_turret.animation.json"));
        assertTrue(geometry.contains("\"name\": \"base\"")
                        && geometry.contains("\"name\": \"head\"")
                        && geometry.contains("\"name\": \"mouth\""),
                "The authored turret pivot hierarchy is incomplete");
        assertTrue(animation.contains("\"firing\"") && animation.contains("\"mouth\""),
                "The turret firing animation no longer drives its jaw");

        BufferedImage idle = ImageIO.read(ASSETS.resolve("textures/block/magic_turret.png").toFile());
        BufferedImage firing = ImageIO.read(ASSETS.resolve("textures/block/magic_turret_firing.png").toFile());
        assertEquals(128, idle.getWidth());
        assertEquals(128, idle.getHeight());
        assertEquals(128, firing.getWidth());
        assertEquals(128 * 6, firing.getHeight(), "The six authored firing frames were not preserved");
    }

    @Test
    void spinosaurusTrophyAndEndgameRecipeRemainWired() throws Exception {
        String trophy = Files.readString(ASSETS.resolve("geckolib/models/block/spinosaurus_head.geo.json"));
        String recipe = Files.readString(Path.of("src/main/resources/data/primevalworks/recipe/magic_turret.json"));
        assertTrue(trophy.contains("\"name\":\"head2\"")
                        && trophy.contains("\"name\":\"upperjaw\"")
                        && trophy.contains("\"name\":\"lowerjaw\""),
                "The placeable trophy no longer uses the authored Spinosaurus head2 hierarchy");
        for (String ingredient : new String[] {
                "spinosaurus_head", "redstone_block", "compressed_ancient_metal_ingot",
                "ancient_spell_ingot", "ancient_spell_stone", "compressed_core"
        }) {
            assertTrue(recipe.contains(ingredient), "Magic Turret recipe lost: " + ingredient);
        }
    }
}
