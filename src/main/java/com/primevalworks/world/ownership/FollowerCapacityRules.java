package com.primevalworks.world.ownership;

public final class FollowerCapacityRules {
    public static final int MINIMUM_SLOTS = 1;
    public static final int MAXIMUM_SLOTS = 4;
    public static final int FIELD_COMMAND_RANKS = 2;

    private FollowerCapacityRules() {
    }

    public static int capacity(int startingSlots, int slotsPerRank, int maximumSlots, int fieldCommandRank) {
        int starting = clamp(startingSlots, MINIMUM_SLOTS, MAXIMUM_SLOTS);
        int maximum = clamp(maximumSlots, starting, MAXIMUM_SLOTS);
        int rank = clamp(fieldCommandRank, 0, FIELD_COMMAND_RANKS);
        int gain = clamp(slotsPerRank, 0, MAXIMUM_SLOTS - MINIMUM_SLOTS);
        return Math.min(maximum, starting + gain * rank);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
