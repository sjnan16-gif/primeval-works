package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DinosaurProgressionTest {
    @Test
    void levelsImproveWorkWithoutErasingSpeciesBalance() {
        assertEquals(1.0F, DinosaurProgression.workDurationMultiplier(1), 0.0001F);
        assertEquals(0.65F, DinosaurProgression.workDurationMultiplier(100), 0.001F);
        assertTrue(DinosaurProgression.healthMultiplier(100) <= 1.301D);
        assertTrue(DinosaurProgression.attackMultiplier(100) <= 1.251D);
        assertTrue(DinosaurProgression.movementMultiplier(100) <= 1.081D);
    }

    @Test
    void experienceCurveAndRewardsRemainBounded() {
        assertEquals(50, DinosaurProgression.experienceForNextLevel(1));
        assertEquals(0, DinosaurProgression.experienceForNextLevel(100));
        assertEquals(18, DinosaurProgression.expeditionExperience(0));
        assertEquals(70, DinosaurProgression.expeditionExperience(4));
        assertEquals(6, DinosaurProgression.workExperience(3));
    }
}
