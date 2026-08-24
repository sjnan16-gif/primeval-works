package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpinosaurusGroundRideRulesTest {
    @Test
    void terrainPitchIsSubtleAndPointsWithTheSlope() {
        assertTrue(SpinosaurusGroundRideRules.terrainPitchDegrees(1.0D, 0.0D, 4.0D) < 0.0F);
        assertTrue(SpinosaurusGroundRideRules.terrainPitchDegrees(0.0D, 1.0D, 4.0D) > 0.0F);
        assertEquals(-12.0F,
                SpinosaurusGroundRideRules.terrainPitchDegrees(4.0D, 0.0D, 1.0D));
    }

    @Test
    void onlyShortForwardDropsKeepGroundSprintMomentum() {
        assertTrue(SpinosaurusGroundRideRules.shouldPreserveDropMomentum(
                true, false, 1.0D, -0.08D, 0.2F, 0.55D));
        assertFalse(SpinosaurusGroundRideRules.shouldPreserveDropMomentum(
                true, false, 1.0D, -0.30D, 1.5F, 0.55D));
        assertFalse(SpinosaurusGroundRideRules.shouldPreserveDropMomentum(
                true, false, 0.0D, -0.08D, 0.2F, 0.55D));
        assertTrue(SpinosaurusGroundRideRules.preservedHorizontalSpeed(0.55D, 1) > 0.49D);
    }
}
