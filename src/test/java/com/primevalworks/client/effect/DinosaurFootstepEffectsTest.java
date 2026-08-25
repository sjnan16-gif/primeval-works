package com.primevalworks.client.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DinosaurFootstepEffectsTest {
    @Test
    void listenerInsideLargeDinosaurBoundsIsAtContactDistance() {
        assertEquals(0.0D, FootstepDistance.toBox(
                0.0D, 3.0D, 0.0D,
                -2.0D, 0.0D, -4.0D, 2.0D, 5.0D, 4.0D
        ));
        assertEquals(2.0D, FootstepDistance.toBox(
                4.0D, 3.0D, 0.0D,
                -2.0D, 0.0D, -4.0D, 2.0D, 5.0D, 4.0D
        ));
    }
}
