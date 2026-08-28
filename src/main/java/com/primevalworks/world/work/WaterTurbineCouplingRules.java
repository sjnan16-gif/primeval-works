package com.primevalworks.world.work;

public final class WaterTurbineCouplingRules {
    public static final int RANGE = 5;
    public static final int MAX_FOLLOWERS = 2;
    private static final float[] OUTPUT_MULTIPLIERS = {0.55F, 0.35F};

    private WaterTurbineCouplingRules() {
    }

    public static float outputMultiplier(int followerIndex) {
        return followerIndex >= 0 && followerIndex < OUTPUT_MULTIPLIERS.length
                ? OUTPUT_MULTIPLIERS[followerIndex] : 0.0F;
    }
}
