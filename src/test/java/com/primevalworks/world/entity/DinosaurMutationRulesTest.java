package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DinosaurMutationRulesTest {
    @Test
    void wildMutationBoundariesAreStable() {
        assertEquals(DinosaurMutationRules.HUGE | DinosaurMutationRules.ALBINO,
                DinosaurMutationRules.roll(false, 0.0F, 0.0F));
        assertEquals(0, DinosaurMutationRules.roll(false,
                DinosaurMutationRules.WILD_HUGE_CHANCE,
                DinosaurMutationRules.WILD_ALBINO_CHANCE));
    }

    @Test
    void premiumIncubationUsesTheAuthoredRareRates() {
        assertEquals(DinosaurMutationRules.HUGE,
                DinosaurMutationRules.roll(true, 0.249F, 0.04F));
        assertEquals(DinosaurMutationRules.ALBINO,
                DinosaurMutationRules.roll(true, 0.25F, 0.039F));
    }

    @Test
    void premiumIncubationIsStrictlyBetterThanWildHatching() {
        assertEquals(0.05F, DinosaurMutationRules.hugeChance(false));
        assertEquals(0.005F, DinosaurMutationRules.albinoChance(false));
        assertEquals(0.25F, DinosaurMutationRules.hugeChance(true));
        assertEquals(0.04F, DinosaurMutationRules.albinoChance(true));
    }

    @Test
    void bredMutationOddsFavorParentTraits() {
        assertEquals(DinosaurMutationRules.HUGE,
                DinosaurMutationRules.rollBred(
                        DinosaurMutationRules.HUGE,
                        0,
                        DinosaurMutationRules.ONE_PARENT_INHERITANCE_CHANCE - 0.001F,
                        1.0F
                ));
        assertEquals(0,
                DinosaurMutationRules.rollBred(
                        DinosaurMutationRules.HUGE,
                        0,
                        DinosaurMutationRules.ONE_PARENT_INHERITANCE_CHANCE,
                        1.0F
                ));
        assertEquals(DinosaurMutationRules.ALBINO,
                DinosaurMutationRules.rollBred(
                        DinosaurMutationRules.ALBINO,
                        DinosaurMutationRules.ALBINO,
                        1.0F,
                        DinosaurMutationRules.TWO_PARENT_INHERITANCE_CHANCE - 0.001F
                ));
    }

    @Test
    void bredEggsStillHaveAControlledChanceForNewTraits() {
        assertEquals(DinosaurMutationRules.HUGE | DinosaurMutationRules.ALBINO,
                DinosaurMutationRules.rollBred(0, 0, 0.0F, 0.0F));
        assertEquals(0,
                DinosaurMutationRules.rollBred(
                        0,
                        0,
                        DinosaurMutationRules.BRED_HUGE_CHANCE,
                        DinosaurMutationRules.BRED_ALBINO_CHANCE
                ));
    }
}
