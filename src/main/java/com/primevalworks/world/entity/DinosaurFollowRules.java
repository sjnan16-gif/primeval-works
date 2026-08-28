package com.primevalworks.world.entity;

public final class DinosaurFollowRules {
    public static final double EMERGENCY_TELEPORT_DISTANCE = 100.0D;
    private static final double RUN_DISTANCE = 8.0D;

    private DinosaurFollowRules() {
    }

    public static boolean shouldEmergencyTeleport(double distanceSquared) {
        return distanceSquared >= EMERGENCY_TELEPORT_DISTANCE * EMERGENCY_TELEPORT_DISTANCE;
    }

    public static boolean shouldRun(double distanceSquared) {
        return distanceSquared > RUN_DISTANCE * RUN_DISTANCE;
    }

    public static float headLeadScale(float headErrorDegrees) {
        return clamp(1.0F - Math.abs(headErrorDegrees) / 105.0F, 0.34F, 1.0F);
    }

    public static float movementTurnScale(float yawErrorDegrees, float bodyErrorDegrees) {
        float worstError = Math.max(Math.abs(yawErrorDegrees), Math.abs(bodyErrorDegrees));
        return clamp(1.0F - worstError / 220.0F, 0.32F, 1.0F);
    }

    public static float locomotionAnimationSpeed(float smoothedMovement, boolean running) {
        float movement = Math.max(0.0F, smoothedMovement);
        return running
                ? clamp(0.82F + movement * 2.45F, 0.88F, 2.20F)
                : clamp(0.74F + movement * 1.85F, 0.78F, 1.55F);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
