package com.primevalworks.world.entity;

public final class DinosaurGeneticPerformanceRules {
    private DinosaurGeneticPerformanceRules() {
    }

    public static float workSpeedMultiplier(int quality) {
        return 0.94F + clamp(quality, 0, 100) * 0.0012F;
    }

    public static float hungerIntervalMultiplier(int quality) {
        return 0.92F + clamp(quality, 0, 100) * 0.0016F;
    }

    public static float moodDrainMultiplier(int quality) {
        return 1.06F - clamp(quality, 0, 100) * 0.0012F;
    }

    public static float passiveStrength(int quality, int level, int mutationMask) {
        float qualityScale = 0.88F + clamp(quality, 0, 100) * 0.0024F;
        float levelScale = 1.0F + (clamp(level, 1, DinosaurProgression.MAX_LEVEL) - 1) * 0.0025F;
        float mutationScale = 1.0F;
        if ((mutationMask & DinosaurMutationRules.HUGE) != 0) mutationScale *= 1.08F;
        if ((mutationMask & DinosaurMutationRules.ALBINO) != 0) mutationScale *= 1.12F;
        return Math.min(1.55F, qualityScale * levelScale * mutationScale);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
