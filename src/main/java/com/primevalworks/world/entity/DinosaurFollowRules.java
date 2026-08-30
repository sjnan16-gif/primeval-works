package com.primevalworks.world.entity;

public final class DinosaurFollowRules {
    public static final int LOCAL_RECOVERY_TICKS = 50;
    public static final int TELEPORT_RECOVERY_TICKS = 140;
    public static final int FAR_TELEPORT_RECOVERY_TICKS = 80;
    public static final int EXTREME_SEPARATION_RECOVERY_TICKS = 100;
    public static final float TURN_LOOP_RECOVERY_DEGREES = 225.0F;
    private static final double RUN_DISTANCE = 8.0D;
    private static final double FAR_RECOVERY_DISTANCE = 32.0D;
    private static final double EXTREME_SEPARATION_DISTANCE = 100.0D;

    private DinosaurFollowRules() {
    }

    public static boolean shouldRun(double distanceSquared) {
        return distanceSquared > RUN_DISTANCE * RUN_DISTANCE;
    }

    public static float headLeadScale(float headErrorDegrees) {
        return clamp(1.0F - Math.abs(headErrorDegrees) / 105.0F, 0.34F, 1.0F);
    }

    public static float movementTurnScale(float yawErrorDegrees, float bodyErrorDegrees) {
        float worstError = Math.max(Math.abs(yawErrorDegrees), Math.abs(bodyErrorDegrees));
        return clamp(1.0F - worstError / 300.0F, 0.56F, 1.0F);
    }

    public static boolean madeMeaningfulProgress(
            double previousDistance,
            double currentDistance,
            double displacementSquared,
            boolean navigationStuck
    ) {
        if (navigationStuck) return false;
        return previousDistance - currentDistance >= 0.12D || displacementSquared >= 0.035D;
    }

    public static boolean madeOwnerCatchupProgress(double previousDistance, double currentDistance) {
        return previousDistance - currentDistance >= 0.18D;
    }

    public static double navigationSampleY(double feetY, double height, boolean inLiquid) {
        return inLiquid ? feetY + height * 0.5D : feetY;
    }

    public static boolean shouldRecoverTurnLoop(float unproductiveTurnDegrees, double distanceSquared) {
        return distanceSquared > RUN_DISTANCE * RUN_DISTANCE
                && unproductiveTurnDegrees >= TURN_LOOP_RECOVERY_DEGREES;
    }

    public static boolean shouldTryLocalRecovery(int stalledTicks) {
        return stalledTicks >= LOCAL_RECOVERY_TICKS;
    }

    public static boolean shouldTeleportAfterStall(int stalledTicks, double distanceSquared) {
        int threshold = distanceSquared >= FAR_RECOVERY_DISTANCE * FAR_RECOVERY_DISTANCE
                ? FAR_TELEPORT_RECOVERY_TICKS
                : TELEPORT_RECOVERY_TICKS;
        return distanceSquared > RUN_DISTANCE * RUN_DISTANCE && stalledTicks >= threshold;
    }

    public static boolean shouldTeleportFollower(
            int stalledTicks,
            int noCatchupTicks,
            int extremeSeparationTicks,
            double distanceSquared
    ) {
        if (shouldTeleportAfterStall(Math.max(stalledTicks, noCatchupTicks), distanceSquared)) return true;
        return distanceSquared >= EXTREME_SEPARATION_DISTANCE * EXTREME_SEPARATION_DISTANCE
                && extremeSeparationTicks >= EXTREME_SEPARATION_RECOVERY_TICKS;
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
