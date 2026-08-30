package com.primevalworks.world.breeding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DinosaurBreedingRulesTest {
    @Test
    void offspringQualityInheritsTheParentAverageWithABoundedBonus() {
        assertEquals(64, DinosaurBreedingRules.inheritedQuality(40, 80, 0));
        assertEquals(70, DinosaurBreedingRules.inheritedQuality(40, 80, 6));
        assertEquals(100, DinosaurBreedingRules.inheritedQuality(100, 100, 6));
    }

    @Test
    void invalidInputsCannotEscapeTheQualityContract() {
        assertEquals(4, DinosaurBreedingRules.inheritedQuality(-20, -10, -5));
        assertEquals(100, DinosaurBreedingRules.inheritedQuality(500, 500, 200));
    }
}
