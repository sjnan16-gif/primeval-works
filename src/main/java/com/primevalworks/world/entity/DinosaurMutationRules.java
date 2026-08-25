package com.primevalworks.world.entity;

import com.primevalworks.config.PrimevalTuning;

/** Birth-only dinosaur mutation rolls shared by wild, bred, and incubated eggs. */
public final class DinosaurMutationRules {
    public static final int HUGE = 1;
    public static final int ALBINO = 2;

    public static final float WILD_HUGE_CHANCE = 0.05F;
    public static final float WILD_ALBINO_CHANCE = 0.005F;
    public static final float INCUBATED_HUGE_CHANCE = 0.25F;
    public static final float INCUBATED_ALBINO_CHANCE = 0.04F;
    public static final float BRED_HUGE_CHANCE = 0.09F;
    public static final float BRED_ALBINO_CHANCE = 0.012F;
    public static final float ONE_PARENT_INHERITANCE_CHANCE = 0.65F;
    public static final float TWO_PARENT_INHERITANCE_CHANCE = 0.88F;

    private DinosaurMutationRules() {
    }

    public static int roll(boolean incubated, float hugeRoll, float albinoRoll) {
        int mask = 0;
        if (hugeRoll < hugeChance(incubated)) mask |= HUGE;
        if (albinoRoll < albinoChance(incubated)) mask |= ALBINO;
        return mask;
    }

    public static float hugeChance(boolean incubated) {
        return incubated ? (float)PrimevalTuning.server().incubatedHugeChance()
                : (float)PrimevalTuning.server().wildHugeChance();
    }

    public static float albinoChance(boolean incubated) {
        return incubated ? (float)PrimevalTuning.server().incubatedAlbinoChance()
                : (float)PrimevalTuning.server().wildAlbinoChance();
    }

    public static boolean inheritsTrait(boolean firstParent, boolean secondParent, float roll, float novelChance) {
        int parentCount = (firstParent ? 1 : 0) + (secondParent ? 1 : 0);
        float chance = switch (parentCount) {
            case 1 -> (float)PrimevalTuning.server().oneParentInheritance();
            case 2 -> (float)PrimevalTuning.server().twoParentInheritance();
            default -> novelChance;
        };
        return roll < chance;
    }

    public static int rollBred(int firstParentMask, int secondParentMask, float hugeRoll, float albinoRoll) {
        int mask = 0;
        if (inheritsTrait((firstParentMask & HUGE) != 0, (secondParentMask & HUGE) != 0,
                hugeRoll, (float)PrimevalTuning.server().bredHugeChance())) {
            mask |= HUGE;
        }
        if (inheritsTrait((firstParentMask & ALBINO) != 0, (secondParentMask & ALBINO) != 0,
                albinoRoll, (float)PrimevalTuning.server().bredAlbinoChance())) {
            mask |= ALBINO;
        }
        return mask;
    }

    /** Mutations lean toward a better birth, without replacing quality as its own roll. */
    public static int qualityBonus(int mutationMask, float roll) {
        int count = Integer.bitCount(mutationMask & (HUGE | ALBINO));
        if (count == 0) return 0;
        int spread = count == 1 ? 3 : 4;
        return (count == 1 ? 2 : 4) + Math.min(spread - 1, (int)(roll * spread));
    }
}
