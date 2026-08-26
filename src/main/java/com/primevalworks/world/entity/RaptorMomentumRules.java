package com.primevalworks.world.entity;

public final class RaptorMomentumRules {
    private static final float BUILD_PER_TICK = 0.032F;
    private static final float LOSS_PER_TICK = 0.075F;

    private RaptorMomentumRules() {
    }

    public static float nextMomentum(float current, boolean running) {
        float change = running ? BUILD_PER_TICK : -LOSS_PER_TICK;
        return clamp(current + change, 0.0F, 1.0F);
    }

    public static float movementMultiplier(float momentum, float passiveStrength) {
        return 1.0F + clamp(momentum, 0.0F, 1.0F) * 0.48F * Math.max(0.0F, passiveStrength);
    }

    public static double pounceHorizontalSpeed(double currentSpeed, float passiveStrength) {
        return Math.max(currentSpeed, 0.54D + Math.min(1.55F, passiveStrength) * 0.08D);
    }

    public static double pounceVerticalSpeed(float passiveStrength) {
        return 0.46D + Math.min(1.55F, passiveStrength) * 0.035D;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
