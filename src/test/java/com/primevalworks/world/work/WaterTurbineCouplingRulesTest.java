package com.primevalworks.world.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WaterTurbineCouplingRulesTest {
    @Test
    void onlyTwoLinkedTurbinesReceiveTheAuthoredRates() {
        assertEquals(2, WaterTurbineCouplingRules.MAX_FOLLOWERS);
        assertEquals(0.55F, WaterTurbineCouplingRules.outputMultiplier(0));
        assertEquals(0.35F, WaterTurbineCouplingRules.outputMultiplier(1));
        assertEquals(0.0F, WaterTurbineCouplingRules.outputMultiplier(2));
    }
}
