package com.primevalworks.world.entity;

import com.primevalworks.config.PrimevalTuning;

public final class DinosaurMutationRules {
    public static final int HUGE = 1;
    public static final int ALBINO = 2;

    public static final float WILD_HUGE_CHANCE = 0.05F;
    public static final float WILD_ALBINO_CHANCE = 0.005F;
    public static final float INCUBATED_HUGE_CHANCE = 0.06F;
    public static final float INCUBATED_ALBINO_CHANCE = 0.01F;
    public static final float BRED_HUGE_CHANCE = 0.09F;
    public static final float BRED_ALBINO_CHANCE = 0.012F;
    public static final float PARENT_INHERITANCE_CHANCE = 0.09F;
    public static final float PARENT_LEVEL_BONUS = 0.06F;
    public static final float PARENT_QUALITY_BONUS = 0.06F;

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

    public static float parentInheritanceChance(int level, int quality) {
        float normalizedLevel = clamp((level - 1) / 99.0F, 0.0F, 1.0F);
        float normalizedQuality = clamp(quality / 100.0F, 0.0F, 1.0F);
        PrimevalTuning.Server tuning = PrimevalTuning.server();
        return clamp((float)tuning.parentMutationInheritanceChance()
                + normalizedLevel * (float)tuning.parentMutationLevelBonus()
                + normalizedQuality * (float)tuning.parentMutationQualityBonus(), 0.0F, 1.0F);
    }

    public static boolean inheritsTrait(
            ParentGenetics first,
            ParentGenetics second,
            int trait,
            TraitRolls rolls,
            float novelChance
    ) {
        boolean firstCarries = (first.mutationMask() & trait) != 0;
        boolean secondCarries = (second.mutationMask() & trait) != 0;
        if (!firstCarries && !secondCarries) return rolls.novel() < novelChance;
        return firstCarries && rolls.firstParent() < parentInheritanceChance(first.level(), first.quality())
                || secondCarries && rolls.secondParent() < parentInheritanceChance(second.level(), second.quality());
    }

    public static int rollBred(
            ParentGenetics first,
            ParentGenetics second,
            TraitRolls hugeRolls,
            TraitRolls albinoRolls
    ) {
        int mask = 0;
        if (inheritsTrait(first, second, HUGE, hugeRolls, (float)PrimevalTuning.server().bredHugeChance())) {
            mask |= HUGE;
        }
        if (inheritsTrait(first, second, ALBINO, albinoRolls, (float)PrimevalTuning.server().bredAlbinoChance())) {
            mask |= ALBINO;
        }
        return mask;
    }

    public static int qualityBonus(int mutationMask, float roll) {
        int count = Integer.bitCount(mutationMask & (HUGE | ALBINO));
        if (count == 0) return 0;
        int spread = count == 1 ? 3 : 4;
        return (count == 1 ? 2 : 4) + Math.min(spread - 1, (int)(roll * spread));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record ParentGenetics(int mutationMask, int level, int quality) {
        public ParentGenetics {
            mutationMask &= HUGE | ALBINO;
            level = Math.max(1, Math.min(100, level));
            quality = Math.max(0, Math.min(100, quality));
        }
    }

    public record TraitRolls(float firstParent, float secondParent, float novel) {
    }
}
