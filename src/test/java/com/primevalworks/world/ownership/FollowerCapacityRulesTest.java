package com.primevalworks.world.ownership;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FollowerCapacityRulesTest {
    @Test
    void defaultsProgressFromOneToThree() {
        assertEquals(1, FollowerCapacityRules.capacity(1, 1, 3, 0));
        assertEquals(2, FollowerCapacityRules.capacity(1, 1, 3, 1));
        assertEquals(3, FollowerCapacityRules.capacity(1, 1, 3, 2));
    }

    @Test
    void serverCanExposeFourSlotsWithoutExceedingUiSupport() {
        assertEquals(4, FollowerCapacityRules.capacity(2, 1, 4, 2));
        assertEquals(4, FollowerCapacityRules.capacity(4, 3, 4, 0));
        assertEquals(4, FollowerCapacityRules.capacity(99, 99, 99, 99));
    }

    @Test
    void startingCapacityWinsOverAnInvalidLowerMaximum() {
        assertEquals(3, FollowerCapacityRules.capacity(3, 0, 1, 0));
        assertEquals(3, FollowerCapacityRules.capacity(3, 2, 1, 2));
    }
}
