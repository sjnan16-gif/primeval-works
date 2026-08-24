package com.primevalworks.world.entity;

public final class DinosaurProgression {
    public static final int MAX_LEVEL = 100;

    private DinosaurProgression() {
    }

    public static int experienceForNextLevel(int level) {
        int clamped = clamp(level, 1, MAX_LEVEL);
        return clamped >= MAX_LEVEL ? 0 : 40 + clamped * 10;
    }

    public static float workDurationMultiplier(int level) {
        return 1.0F - Math.min(0.35F, (clamp(level, 1, MAX_LEVEL) - 1) * 0.00355F);
    }

    public static double healthMultiplier(int level) {
        return 1.0D + Math.min(0.30D, (clamp(level, 1, MAX_LEVEL) - 1) * 0.00305D);
    }

    public static double attackMultiplier(int level) {
        return 1.0D + Math.min(0.25D, (clamp(level, 1, MAX_LEVEL) - 1) * 0.00255D);
    }

    public static double movementMultiplier(int level) {
        return 1.0D + Math.min(0.08D, (clamp(level, 1, MAX_LEVEL) - 1) * 0.00081D);
    }

    public static int workExperience(int jobIndex) {
        return switch (clamp(jobIndex, 0, 4)) {
            case 0 -> 3;
            case 1, 2 -> 4;
            case 3 -> 6;
            default -> 5;
        };
    }

    public static int expeditionExperience(int tier) {
        return 18 + clamp(tier, 0, 4) * 13;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
