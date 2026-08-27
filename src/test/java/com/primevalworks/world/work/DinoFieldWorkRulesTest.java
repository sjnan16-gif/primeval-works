package com.primevalworks.world.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DinoFieldWorkRulesTest {
    @Test
    void whistleRangeIsAlwaysKeptInsideTheServerLimit() {
        assertEquals(16, DinoWhistleRules.clampRange(-50));
        assertEquals(85, DinoWhistleRules.clampRange(500));
        assertEquals(40, DinoWhistleRules.clampRange(40));
    }

    @Test
    void areaOrdersRejectHugeVolumesAndLongThinSelections() {
        assertTrue(DinoFieldWorkLimits.areaWithinLimits(0, 0, 0, 7, 7, 7));
        assertFalse(DinoFieldWorkLimits.areaWithinLimits(0, 0, 0, 8, 8, 8));
        assertFalse(DinoFieldWorkLimits.areaWithinLimits(0, 0, 0, 16, 0, 0));
    }

    @Test
    void connectedAndAreaCapsStayBounded() {
        assertEquals(64, DinoFieldWorkLimits.MAX_CONNECTED_BLOCKS);
        assertEquals(512, DinoFieldWorkLimits.MAX_AREA_BLOCKS);
        assertEquals(16, DinoFieldWorkLimits.MAX_AREA_SPAN);
    }

    @Test
    void commandModesHaveClearPlayerFacingExplanations() {
        for (DinosaurCommandMode mode : DinosaurCommandMode.values()) {
            assertFalse(mode.title().isBlank());
            assertTrue(mode.description().length() >= 24);
        }
    }
}
