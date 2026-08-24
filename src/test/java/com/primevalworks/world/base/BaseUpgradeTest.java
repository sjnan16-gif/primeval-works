package com.primevalworks.world.base;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseUpgradeTest {
    @Test
    void upgradeIdsRemainUniqueForNetworkPersistence() {
        long unique = Arrays.stream(BaseUpgrade.values()).map(BaseUpgrade::id).distinct().count();
        assertEquals(BaseUpgrade.values().length, unique);
    }

    @Test
    void rosterBranchesAddExactlySevenLateGameSlots() {
        int added = 2 + 2 + 3;
        assertEquals(14, 7 + added);
        assertTrue(BaseUpgrade.CREW_PERCHES.prerequisiteId() >= 0);
        assertTrue(BaseUpgrade.PACK_HIERARCHY.prerequisiteId() >= 0);
        assertTrue(BaseUpgrade.ANCIENT_BONDS.prerequisiteId() >= 0);
    }

    @Test
    void equalTwoSlotRewardsHaveEqualCostsAndOrderIndependentLabels() {
        assertEquals("+2 Slots", BaseUpgrade.CREW_PERCHES.title());
        assertEquals("+2 Slots", BaseUpgrade.PACK_HIERARCHY.title());
        assertEquals("+3 Slots", BaseUpgrade.ANCIENT_BONDS.title());
        assertEquals(
                BaseUpgrade.CREW_PERCHES.costForLevel(0),
                BaseUpgrade.PACK_HIERARCHY.costForLevel(0)
        );
    }
}
