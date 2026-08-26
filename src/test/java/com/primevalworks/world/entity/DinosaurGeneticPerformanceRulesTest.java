package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DinosaurGeneticPerformanceRulesTest {
    @Test
    void qualityImprovesWorkAndCareWithoutDominatingSpecies() {
        assertTrue(DinosaurGeneticPerformanceRules.workSpeedMultiplier(100)
                > DinosaurGeneticPerformanceRules.workSpeedMultiplier(0));
        assertTrue(DinosaurGeneticPerformanceRules.hungerIntervalMultiplier(100)
                > DinosaurGeneticPerformanceRules.hungerIntervalMultiplier(0));
        assertTrue(DinosaurGeneticPerformanceRules.moodDrainMultiplier(100)
                < DinosaurGeneticPerformanceRules.moodDrainMultiplier(0));
        assertTrue(DinosaurGeneticPerformanceRules.workSpeedMultiplier(100) <= 1.06F);
    }

    @Test
    void qualityLevelAndMutationsAllStrengthenPassives() {
        float baseline = DinosaurGeneticPerformanceRules.passiveStrength(50, 1, 0);
        assertTrue(DinosaurGeneticPerformanceRules.passiveStrength(100, 1, 0) > baseline);
        assertTrue(DinosaurGeneticPerformanceRules.passiveStrength(50, 100, 0) > baseline);
        assertTrue(DinosaurGeneticPerformanceRules.passiveStrength(
                50, 1, DinosaurMutationRules.HUGE | DinosaurMutationRules.ALBINO) > baseline);
    }
}
