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

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
