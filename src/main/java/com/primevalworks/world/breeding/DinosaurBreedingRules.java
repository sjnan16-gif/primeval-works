package com.primevalworks.world.breeding;

public final class DinosaurBreedingRules {
    private DinosaurBreedingRules() {
    }

    public static int inheritedQuality(int firstParentQuality, int secondParentQuality, int improvementRoll) {
        int first = clamp(firstParentQuality, 0, 100);
        int second = clamp(secondParentQuality, 0, 100);
        int improvement = 4 + clamp(improvementRoll, 0, 6);
        return clamp((first + second) / 2 + improvement, 0, 100);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
