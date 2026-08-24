package com.primevalworks.client.render.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AlbinoTextureManagerTest {
    @Test
    void speciesEyeMasksRejectKnownNonEyeTextureAccents() {
        assertTrue(AlbinoEyeMasks.contains("textures/entity/spino.png", 192, 183));
        assertTrue(AlbinoEyeMasks.contains("textures/entity/spino.png", 222, 183));
        assertFalse(AlbinoEyeMasks.contains("textures/entity/spino.png", 185, 93));

        assertTrue(AlbinoEyeMasks.contains("textures/entity/stegosaurus.png", 108, 52));
        assertFalse(AlbinoEyeMasks.contains("textures/entity/stegosaurus.png", 95, 122));
        assertFalse(AlbinoEyeMasks.contains("textures/entity/parasaurolophus.png", 100, 100));
    }

    @Test
    void authoredSaddlePixelsAreKeptOutOfTheAlbinoRecolour() {
        assertTrue(AlbinoEquipmentMask.contains(0xFF19302A, 0xFF6F4A2D));
        assertFalse(AlbinoEquipmentMask.contains(0xFF19302A, 0xFF19302A));
    }
}
