package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DinosaurNeedsRulesTest {
    @Test
    void repeatedWorkRequestsCannotDrainPastTheSharedClock() {
        DinosaurNeedsRules.DrainResult initialized = DinosaurNeedsRules.hungerDrain(1_000L, 0L, 320, 1.0F);
        assertFalse(initialized.drain());
        assertEquals(1_320L, initialized.nextDrainTick());

        for (long tick = 1_001L; tick < 1_320L; tick++) {
            DinosaurNeedsRules.DrainResult blocked = DinosaurNeedsRules.hungerDrain(
                    tick, initialized.nextDrainTick(), 320, 1.0F);
            assertFalse(blocked.drain());
            assertEquals(1_320L, blocked.nextDrainTick());
        }

        DinosaurNeedsRules.DrainResult due = DinosaurNeedsRules.hungerDrain(1_320L, 1_320L, 320, 1.0F);
        assertTrue(due.drain());
        assertEquals(1_640L, due.nextDrainTick());
    }

    @Test
    void careUpgradesExtendRatherThanBypassTheCap() {
        DinosaurNeedsRules.DrainResult upgraded = DinosaurNeedsRules.hungerDrain(400L, 0L, 500, 1.16F);
        assertEquals(980L, upgraded.nextDrainTick());
    }
}
