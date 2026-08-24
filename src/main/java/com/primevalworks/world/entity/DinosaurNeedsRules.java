package com.primevalworks.world.entity;

public final class DinosaurNeedsRules {
    private DinosaurNeedsRules() {
    }

    public static DrainResult hungerDrain(long gameTime, long nextDrainTick, int speciesInterval, float baseMultiplier) {
        int interval = Math.max(1, Math.round(speciesInterval * Math.max(0.1F, baseMultiplier)));
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
