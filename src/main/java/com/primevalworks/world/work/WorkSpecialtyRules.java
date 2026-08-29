package com.primevalworks.world.work;

import com.primevalworks.config.PrimevalTuning;

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
    private static final float ENERGY_BALANCE_MULTIPLIER = 0.85F;
    private static final int[][] EXPEDITION_RISKS = {
            {18, 15, 8, 4, 1},
            {35, 30, 18, 8, 3},
            {55, 48, 31, 15, 6},
            {78, 72, 48, 28, 9},
            {100, 100, 92, 64, 12}
    };
    private static final int[][] EXPEDITION_DURATION_MINUTES = {
            {50, 38, 26, 16, 10},
            {70, 55, 38, 24, 14},
            {95, 75, 52, 33, 19},
            {130, 105, 76, 48, 26},
            {180, 180, 120, 64, 34}
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
        return expeditionDurationMinutes(tier, 4, 1.0F);
    }

    public static long expeditionDurationTicks(int tier) {
        return expeditionDurationMinutes(tier) * 60L * 20L;
    }

    public static int expeditionRiskPercent(int tier) {
        return expeditionRiskPercent(tier, 4, 1, 1.0F);
    }

    public static int expeditionRiskPercent(int tier, int stars, int level) {
        return expeditionRiskPercent(tier, stars, level, 1.0F);
    }

    public static int expeditionRiskPercent(int tier, int stars, int level, float mutationMultiplier) {
        int risk = EXPEDITION_RISKS[clampTier(tier)][clampStars(stars)];
        if (!canAttemptExpedition(tier, stars)) return 100;
        float mutationBenefit = Math.max(1.0F, mutationMultiplier);
        return Math.max(0, Math.min(100, Math.round(risk / mutationBenefit
                * (float)PrimevalTuning.server().expeditionRisk())));
    }

    public static boolean canAttemptExpedition(int tier, int stars) {
        return clampTier(tier) < 4 || clampStars(stars) >= 2;
    }

    public static int expeditionRewardCount(int tier) {
        return Math.max(0, Math.round(EXPEDITION_REWARDS[clampTier(tier)]
                * (float)PrimevalTuning.server().expeditionRewards()));
    }

    public static int efficiencyPercent(int stars) {
        return EFFICIENCY_PERCENT[clampStars(stars)];
    }

    public static float energyPerSecond(int stars) {
        return ENERGY_PER_SECOND[clampStars(stars)]
                * ENERGY_BALANCE_MULTIPLIER
                * (float)PrimevalTuning.server().energyGeneration();
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
        int aptitudeLimit = Math.max(1, (int)Math.floor(maximumStackSize * specialtyFraction * levelBonus
                * PrimevalTuning.server().transportCapacity()));
        return Math.min(Math.min(requestedBatch, maximumStackSize), aptitudeLimit);
    }

    public static int actionDurationTicks(int baseTicks, int stars) {
        if (baseTicks <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(baseTicks * 100.0D / efficiencyPercent(stars)
                / PrimevalTuning.server().workSpeed()));
    }

    public static int workMoodDrainUnitsPerTick(int schedule) {
        double rate = PrimevalTuning.server().moodDrainRate();
        if (rate <= 0.0D) return 0;
        double scheduleMultiplier = schedule == 2 ? PrimevalTuning.server().nightShiftMoodRate() : 1.0D;
        return Math.max(1, (int)Math.round(20.0D * rate * scheduleMultiplier));
    }

    public static long expeditionDurationTicks(int tier, int stars) {
        return expeditionDurationTicks(tier, stars, 1.0F);
    }

    public static long expeditionDurationTicks(int tier, int stars, float mutationMultiplier) {
        int minutes = EXPEDITION_DURATION_MINUTES[clampTier(tier)][clampStars(stars)];
        float mutationBenefit = Math.max(1.0F, mutationMultiplier);
        return Math.max(1L, Math.round(minutes * 60.0D * 20.0D / mutationBenefit
                * PrimevalTuning.server().expeditionTime()));
    }

    public static int expeditionDurationMinutes(int tier, int stars) {
        return expeditionDurationMinutes(tier, stars, 1.0F);
    }

    public static int expeditionDurationMinutes(int tier, int stars, float mutationMultiplier) {
        return (int) Math.ceil(expeditionDurationTicks(tier, stars, mutationMultiplier) / 1200.0D);
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
