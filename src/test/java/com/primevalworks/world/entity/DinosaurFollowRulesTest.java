package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DinosaurFollowRulesTest {
    @Test
    void followersRunBeforeEmergencyTeleporting() {
        assertTrue(DinosaurFollowRules.shouldRun(9.0D * 9.0D));
        assertFalse(DinosaurFollowRules.shouldEmergencyTeleport(99.99D * 99.99D));
        assertTrue(DinosaurFollowRules.shouldEmergencyTeleport(100.0D * 100.0D));
    }

    @Test
    void aHardTurnSlowsContinuouslyWithoutFreezingMovement() {
        float gentle = DinosaurFollowRules.movementTurnScale(20.0F, 12.0F);
        float hard = DinosaurFollowRules.movementTurnScale(150.0F, 120.0F);
        assertTrue(gentle > hard);
        assertTrue(hard >= 0.32F);
        assertTrue(Math.abs(DinosaurFollowRules.movementTurnScale(80.0F, 60.0F)
                - DinosaurFollowRules.movementTurnScale(81.0F, 60.0F)) < 0.01F);
    }

    @Test
    void headLeadNeverLocksTheBody() {
        assertEquals(1.0F, DinosaurFollowRules.headLeadScale(0.0F));
        assertTrue(DinosaurFollowRules.headLeadScale(80.0F) < 1.0F);
        assertEquals(0.34F, DinosaurFollowRules.headLeadScale(180.0F));
    }
}
