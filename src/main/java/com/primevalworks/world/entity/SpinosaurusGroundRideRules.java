package com.primevalworks.world.entity;

public final class SpinosaurusGroundRideRules {
    private static final float MAX_TERRAIN_PITCH_DEGREES = 12.0F;
    private static final int DROP_MOMENTUM_GRACE_TICKS = 8;

    private SpinosaurusGroundRideRules() {
    }

    public static float terrainPitchDegrees(double frontHeight, double rearHeight, double sampleSpan) {
        if (!Double.isFinite(frontHeight) || !Double.isFinite(rearHeight) || sampleSpan <= 0.0D) return 0.0F;
        float pitch = (float)-Math.toDegrees(Math.atan2(frontHeight - rearHeight, sampleSpan));
        return Math.max(-MAX_TERRAIN_PITCH_DEGREES, Math.min(MAX_TERRAIN_PITCH_DEGREES, pitch));
    }

    public static int dropMomentumGraceTicks() {
        return DROP_MOMENTUM_GRACE_TICKS;
    }

    public static boolean shouldPreserveDropMomentum(
            boolean wasGrounded,
            boolean isGrounded,
            double forwardInput,
            double verticalSpeed,
            double fallDistance,
            double horizontalSpeed
    ) {
        return wasGrounded
                && !isGrounded
                && forwardInput > 0.05D
                && verticalSpeed <= 0.08D
                && fallDistance <= 1.35F
                && horizontalSpeed > 0.08D;
    }

    public static double preservedHorizontalSpeed(double entrySpeed, int remainingTicks) {
        int elapsed = Math.max(0, DROP_MOMENTUM_GRACE_TICKS - remainingTicks);
        return Math.max(0.0D, entrySpeed) * Math.pow(0.985D, elapsed);
    }
}
