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

    public static boolean pursuitActive(boolean transporting, boolean hunting, boolean pouncing) {
        return transporting || hunting || pouncing;
    }

    public static float movementMultiplier(float momentum, float passiveStrength) {
        return 1.0F + clamp(momentum, 0.0F, 1.0F) * 0.48F * Math.max(0.0F, passiveStrength);
    }

    public static float turnSpeedMultiplier(float yawError, float bodyError) {
        float error = Math.max(Math.abs(yawError), Math.abs(bodyError) * 1.15F);
        float normalized = clamp((error - 18.0F) / 72.0F, 0.0F, 1.0F);
        float eased = normalized * normalized * (3.0F - 2.0F * normalized);
        return 1.0F - eased * 0.44F;
    }

    public static float steeringResponse(float momentum, float yawError) {
        float speedStability = 1.0F - clamp(momentum, 0.0F, 1.0F) * 0.52F;
        float turnDemand = clamp(Math.abs(yawError) / 90.0F, 0.0F, 1.0F);
        return clamp((0.18F + turnDemand * 0.10F) * speedStability, 0.085F, 0.28F);
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
