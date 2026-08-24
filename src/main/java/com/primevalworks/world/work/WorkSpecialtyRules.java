package com.primevalworks.world.work;

public final class WorkSpecialtyRules {
    public static final int WORK_MOOD_DRAIN_UNITS_PER_POINT = 12_000;
    public static final int CHEST_EXTRACT_TICKS = 28;
    public static final int CHEST_INSERT_TICKS = 28;
    public static final int LOOSE_ITEM_PICKUP_TICKS = 28;
    public static final int FIRE_TENDING_TICKS = 80;
    public static final int ORE_PROCESSING_TICKS = 160;
    public static final int ENERGY_GENERATION_TICKS = 60;
    public static final int CRAFTING_TICKS = 100;
    private static final int[] EFFICIENCY_PERCENT = {20, 20, 45, 65, 100};
    private static final float[] ENERGY_PER_SECOND = {1.5F, 2.0F, 4.5F, 7.5F, 11.0F};
    private static final int[][] EXPEDITION_RISKS = {
            {4, 2, 1, 0, 0},
            {35, 35, 22, 12, 6},
            {70, 70, 55, 35, 18},
            {98, 97, 96, 95, 94},
            {100, 100, 100, 100, 98}
    };
    private static final int[] EXPEDITION_REWARDS = {1, 2, 3, 4, 5};

    private WorkSpecialtyRules() {
    }

    public static int itemFilterCapacity(int jobIndex) {
        return switch (jobIndex) {
            case 0 -> 4;
            case 1 -> 3;
            case 2 -> 0;
            case 3 -> 1;
            default -> 0;
        };
    }

    public static int fuelFilterCapacity(int jobIndex) {
        return jobIndex == 1 ? 4 : 0;
    }

    public static int expeditionDurationMinutes(int tier) {
        return 10 + clampTier(tier) * 5;
    }

    public static long expeditionDurationTicks(int tier) {
        return expeditionDurationMinutes(tier) * 60L * 20L;
    }

    public static int expeditionRiskPercent(int tier) {
        return expeditionRiskPercent(tier, 4, 1);
    }

    public static int expeditionRiskPercent(int tier, int stars, int level) {
        int risk = EXPEDITION_RISKS[clampTier(tier)][clampStars(stars)];
        int levelReduction = Math.min(8, Math.max(0, level - 1) / 12);
        return Math.max(0, risk - levelReduction);
    }

    public static boolean canAttemptExpedition(int tier, int stars) {
        return clampTier(tier) < 4 || clampStars(stars) == 4;
    }

    public static int expeditionRewardCount(int tier) {
        return EXPEDITION_REWARDS[clampTier(tier)];
    }

    public static int efficiencyPercent(int stars) {
        return EFFICIENCY_PERCENT[clampStars(stars)];
    }

    public static float energyPerSecond(int stars) {
        return ENERGY_PER_SECOND[clampStars(stars)];
    }

    public static float energyPerSecond(int stars, int level) {
        float levelBonus = 1.0F + Math.min(0.12F, Math.max(0, level - 1) * 0.00122F);
        return energyPerSecond(stars) * levelBonus;
    }

    public static int transportCapacity(int stars, int level, int requestedBatch, int maximumStackSize) {
        if (requestedBatch <= 0 || maximumStackSize <= 0) return 0;
        float specialtyFraction = switch (clampStars(stars)) {
            case 0 -> 0.08F;
            case 1 -> 0.15F;
            case 2 -> 0.34F;
            case 3 -> 0.65F;
            default -> 1.0F;
        };
        float levelBonus = 1.0F + Math.min(0.20F, Math.max(0, level - 1) * 0.00203F);
        int aptitudeLimit = Math.max(1, (int)Math.floor(maximumStackSize * specialtyFraction * levelBonus));
        return Math.min(Math.min(requestedBatch, maximumStackSize), aptitudeLimit);
    }

    public static int actionDurationTicks(int baseTicks, int stars) {
        if (baseTicks <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(baseTicks * 100.0D / efficiencyPercent(stars)));
    }

    public static int workMoodDrainUnitsPerTick(int schedule) {
        return schedule == 2 ? 46 : 20;
    }

    public static long expeditionDurationTicks(int tier, int stars) {
        return expeditionDurationTicks(tier);
    }

    public static int expeditionDurationMinutes(int tier, int stars) {
        return (int) Math.ceil(expeditionDurationTicks(tier, stars) / 1200.0D);
    }

    public static double transportMovementMultiplier(int stars, int carriedCount, int maximumStackSize) {
        if (carriedCount <= 0 || maximumStackSize <= 0) {
            return 1.0D;
        }
        double load = Math.min(1.0D, carriedCount / (double) maximumStackSize);
        double fullLoadPenalty = switch (clampStars(stars)) {
            case 0, 1 -> 0.30D;
            case 2 -> 0.22D;
            case 3 -> 0.14D;
            default -> 0.08D;
        };
        return 1.0D - load * fullLoadPenalty;
    }

    public static int transportHandlingDurationTicks(int baseTicks, int stars, int carriedCount, int maximumStackSize) {
        double load = maximumStackSize <= 0 ? 0.0D : Math.min(1.0D, Math.max(0, carriedCount) / (double) maximumStackSize);
        double loadPenalty = 0.5D * load * (1.0D - clampStars(stars) / 4.0D);
        int loadedBaseTicks = Math.max(1, (int) Math.round(baseTicks * (1.0D + loadPenalty)));
        return actionDurationTicks(loadedBaseTicks, stars);
    }

    private static int clampTier(int tier) {
        return Math.max(0, Math.min(4, tier));
    }

    private static int clampStars(int stars) {
        return Math.max(0, Math.min(4, stars));
    }
}
