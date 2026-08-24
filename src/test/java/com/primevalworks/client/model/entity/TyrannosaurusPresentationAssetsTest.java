package com.primevalworks.client.model.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TyrannosaurusPresentationAssetsTest {
    @Test
    void attackLeavesLegsToTheLocomotionController() throws Exception {
        String json = Files.readString(Path.of(
                "src/main/resources/assets/primevalworks/geckolib/animations/entity/t_rex.animation.json"));
        int attackStart = json.indexOf("\"attack\"");
        int nextAnimation = json.indexOf("\"turnleft\"", attackStart);
        assertTrue(attackStart >= 0 && nextAnimation > attackStart, "Missing Tyrannosaurus attack clip");
        String attack = json.substring(attackStart, nextAnimation);
        assertFalse(attack.contains("\"rightleg\""), "Attack clip overrides the moving right leg");
        assertFalse(attack.contains("\"leftleg\""), "Attack clip overrides the moving left leg");
        assertTrue(attack.contains("\"head2\"") && attack.contains("\"lowerjaw\""),
                "Attack lost its authored upper-body motion");
    }
}
