package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DinosaurFollowRulesTest {
    @Test
    void followersRunWhenTheirOwnerGetsFarAway() {
        assertFalse(DinosaurFollowRules.shouldRun(7.99D * 7.99D));
        assertTrue(DinosaurFollowRules.shouldRun(9.0D * 9.0D));
    }

    @Test
    void aHardTurnSlowsContinuouslyWithoutFreezingMovement() {
        float gentle = DinosaurFollowRules.movementTurnScale(20.0F, 12.0F);
        float hard = DinosaurFollowRules.movementTurnScale(150.0F, 120.0F);
        assertTrue(gentle > hard);
        assertTrue(hard >= 0.56F);
        assertTrue(Math.abs(DinosaurFollowRules.movementTurnScale(80.0F, 60.0F)
                - DinosaurFollowRules.movementTurnScale(81.0F, 60.0F)) < 0.01F);
    }

    @Test
    void movementAroundAnObstacleStillCountsAsProgress() {
        assertTrue(DinosaurFollowRules.madeMeaningfulProgress(18.0D, 18.1D, 0.09D, false));
        assertTrue(DinosaurFollowRules.madeMeaningfulProgress(18.0D, 17.7D, 0.0D, false));
        assertFalse(DinosaurFollowRules.madeMeaningfulProgress(18.0D, 17.98D, 0.001D, false));
        assertFalse(DinosaurFollowRules.madeMeaningfulProgress(18.0D, 17.0D, 1.0D, true));
    }

    @Test
    void teleportIsReservedForSustainedNavigationFailure() {
        assertFalse(DinosaurFollowRules.shouldTeleportAfterStall(139, 12.0D * 12.0D));
        assertTrue(DinosaurFollowRules.shouldTeleportAfterStall(140, 12.0D * 12.0D));
        assertFalse(DinosaurFollowRules.shouldTeleportAfterStall(79, 40.0D * 40.0D));
        assertTrue(DinosaurFollowRules.shouldTeleportAfterStall(80, 40.0D * 40.0D));
        assertFalse(DinosaurFollowRules.shouldTeleportAfterStall(500, 5.0D * 5.0D));
    }

    @Test
    void headLeadNeverLocksTheBody() {
        assertEquals(1.0F, DinosaurFollowRules.headLeadScale(0.0F));
        assertTrue(DinosaurFollowRules.headLeadScale(80.0F) < 1.0F);
        assertEquals(0.34F, DinosaurFollowRules.headLeadScale(180.0F));
    }

    @Test
    void followerGaitPlaybackTracksMovementAndStaysBounded() {
        float slowWalk = DinosaurFollowRules.locomotionAnimationSpeed(0.05F, false);
        float fastWalk = DinosaurFollowRules.locomotionAnimationSpeed(0.35F, false);
        float slowRun = DinosaurFollowRules.locomotionAnimationSpeed(0.05F, true);
        float fastRun = DinosaurFollowRules.locomotionAnimationSpeed(0.70F, true);

        assertTrue(fastWalk > slowWalk);
        assertTrue(fastRun > slowRun);
        assertTrue(slowWalk >= 0.78F && fastWalk <= 1.55F);
        assertTrue(slowRun >= 0.88F && fastRun <= 2.20F);
    }
}
