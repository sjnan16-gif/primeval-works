package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RaptorMomentumRulesTest {
    @Test
    void uninterruptedRunningBuildsSpeedAndStoppingBleedsItAway() {
        float momentum = 0.0F;
        for (int tick = 0; tick < 40; tick++) momentum = RaptorMomentumRules.nextMomentum(momentum, true);
        assertEquals(1.0F, momentum);
        assertTrue(RaptorMomentumRules.movementMultiplier(momentum, 1.0F) > 1.4F);
        for (int tick = 0; tick < 20; tick++) momentum = RaptorMomentumRules.nextMomentum(momentum, false);
        assertEquals(0.0F, momentum);
    }

    @Test
    void pounceKeepsExistingChaseMomentum() {
        assertTrue(RaptorMomentumRules.pounceHorizontalSpeed(0.80D, 1.0F) >= 0.80D);
        assertTrue(RaptorMomentumRules.pounceVerticalSpeed(1.0F) > 0.45D);
    }

    @Test
    void pursuitOnlyActivatesForTransportAndCombat() {
        assertTrue(!RaptorMomentumRules.pursuitActive(false, false, false));
        assertTrue(RaptorMomentumRules.pursuitActive(true, false, false));
        assertTrue(RaptorMomentumRules.pursuitActive(false, true, false));
        assertTrue(RaptorMomentumRules.pursuitActive(false, false, true));
    }

    @Test
    void turningSlowsContinuouslyWithoutStoppingTheRaptor() {
        assertEquals(1.0F, RaptorMomentumRules.turnSpeedMultiplier(12.0F, 8.0F), 0.0001F);
        float mediumTurn = RaptorMomentumRules.turnSpeedMultiplier(48.0F, 34.0F);
        float hardTurn = RaptorMomentumRules.turnSpeedMultiplier(90.0F, 70.0F);
        assertTrue(mediumTurn < 1.0F && mediumTurn > hardTurn);
        assertTrue(hardTurn >= 0.54F, "A hard turn must not zero velocity and restart the gait");
        assertTrue(Math.abs(RaptorMomentumRules.turnSpeedMultiplier(48.0F, 34.0F)
                - RaptorMomentumRules.turnSpeedMultiplier(49.0F, 34.0F)) < 0.03F,
                "Adjacent yaw values must not cross a stepped braking threshold");
    }
}
