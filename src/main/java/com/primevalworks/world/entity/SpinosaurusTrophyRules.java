package com.primevalworks.world.entity;

import net.minecraft.util.RandomSource;

public final class SpinosaurusTrophyRules {
    private static final float MANUAL_KILL_CHANCE = 0.08F;
    private static final float FRONTIER_EXPEDITION_CHANCE = 0.018F;

    private SpinosaurusTrophyRules() {
    }

    public static boolean rollsManualKillDrop(RandomSource random) {
        return random.nextFloat() < MANUAL_KILL_CHANCE;
    }

    public static float manualKillChance() {
        return MANUAL_KILL_CHANCE;
    }

    public static float frontierExpeditionChance() {
        return FRONTIER_EXPEDITION_CHANCE;
    }
}
