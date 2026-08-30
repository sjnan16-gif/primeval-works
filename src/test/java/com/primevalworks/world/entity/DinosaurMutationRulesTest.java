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
                DinosaurMutationRules.roll(true, 0.059F, 0.01F));
        assertEquals(DinosaurMutationRules.ALBINO,
                DinosaurMutationRules.roll(true, 0.06F, 0.009F));
        assertEquals(0, DinosaurMutationRules.roll(true, 0.06F, 0.01F));
    }

    @Test
    void parentInheritanceStartsAtNinePercentAndScalesWithParentStats() {
        assertEquals(0.09F, DinosaurMutationRules.parentInheritanceChance(1, 0), 0.0001F);
        assertEquals(0.21F, DinosaurMutationRules.parentInheritanceChance(100, 100), 0.0001F);
    }

    @Test
    void eachMutationCarryingParentGetsAnIndependentInheritanceRoll() {
        DinosaurMutationRules.ParentGenetics lowParent = parent(DinosaurMutationRules.HUGE, 1, 0);
        DinosaurMutationRules.ParentGenetics clearParent = parent(0, 1, 0);
        assertEquals(DinosaurMutationRules.HUGE, DinosaurMutationRules.rollBred(
                lowParent, clearParent,
                rolls(0.089F, 1.0F, 1.0F), rolls(1.0F, 1.0F, 1.0F)));
        assertEquals(0, DinosaurMutationRules.rollBred(
                lowParent, clearParent,
                rolls(0.09F, 1.0F, 1.0F), rolls(1.0F, 1.0F, 1.0F)));

        DinosaurMutationRules.ParentGenetics secondCarrier = parent(DinosaurMutationRules.HUGE, 1, 0);
        assertEquals(DinosaurMutationRules.HUGE, DinosaurMutationRules.rollBred(
                lowParent, secondCarrier,
                rolls(1.0F, 0.089F, 1.0F), rolls(1.0F, 1.0F, 1.0F)));
    }

    @Test
    void highLevelHighQualityParentsReceiveTheScaledChance() {
        DinosaurMutationRules.ParentGenetics strongParent = parent(DinosaurMutationRules.ALBINO, 100, 100);
        assertEquals(DinosaurMutationRules.ALBINO, DinosaurMutationRules.rollBred(
                strongParent, parent(0, 1, 0),
                rolls(1.0F, 1.0F, 1.0F), rolls(0.209F, 1.0F, 1.0F)));
        assertEquals(0, DinosaurMutationRules.rollBred(
                strongParent, parent(0, 1, 0),
                rolls(1.0F, 1.0F, 1.0F), rolls(0.211F, 1.0F, 1.0F)));
    }

    @Test
    void bredEggsRetainControlledNovelMutationOdds() {
        DinosaurMutationRules.ParentGenetics clear = parent(0, 1, 0);
        assertEquals(DinosaurMutationRules.HUGE | DinosaurMutationRules.ALBINO,
                DinosaurMutationRules.rollBred(
                        clear, clear, rolls(1.0F, 1.0F, 0.0F), rolls(1.0F, 1.0F, 0.0F)));
        assertEquals(0, DinosaurMutationRules.rollBred(
                clear, clear,
                rolls(1.0F, 1.0F, DinosaurMutationRules.BRED_HUGE_CHANCE),
                rolls(1.0F, 1.0F, DinosaurMutationRules.BRED_ALBINO_CHANCE)));
    }

    private static DinosaurMutationRules.ParentGenetics parent(int mutations, int level, int quality) {
        return new DinosaurMutationRules.ParentGenetics(mutations, level, quality);
    }

    private static DinosaurMutationRules.TraitRolls rolls(float first, float second, float novel) {
        return new DinosaurMutationRules.TraitRolls(first, second, novel);
    }
}
