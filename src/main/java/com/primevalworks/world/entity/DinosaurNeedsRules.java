package com.primevalworks.world.entity;

import com.primevalworks.config.PrimevalTuning;

public final class DinosaurNeedsRules {
    private DinosaurNeedsRules() {
    }

    public static DrainResult hungerDrain(long gameTime, long nextDrainTick, int speciesInterval, float baseMultiplier) {
        double rate = PrimevalTuning.server().hungerDrainRate();
        if (rate <= 0.0D) return new DrainResult(false, gameTime + 20L);
        int interval = Math.max(1, (int)Math.round(speciesInterval * Math.max(0.1F, baseMultiplier) / rate));
        if (nextDrainTick <= 0L) {
            return new DrainResult(false, gameTime + interval);
        }
        if (gameTime < nextDrainTick) {
            return new DrainResult(false, nextDrainTick);
        }
        return new DrainResult(true, gameTime + interval);
    }

    public record DrainResult(boolean drain, long nextDrainTick) {
    }
}
