package com.primevalworks.world.work;

public final class DinoWhistleRules {
    public static final int MIN_RANGE = 16;
    public static final int MAX_RANGE = 85;

    private DinoWhistleRules() {}

    public static int clampRange(int range) {
        return Math.max(MIN_RANGE, Math.min(MAX_RANGE, range));
    }
}
