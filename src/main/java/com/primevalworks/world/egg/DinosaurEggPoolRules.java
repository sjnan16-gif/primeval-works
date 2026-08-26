package com.primevalworks.world.egg;

import com.primevalworks.world.entity.DinosaurSpecies;

import java.util.Arrays;

public final class DinosaurEggPoolRules {
    private DinosaurEggPoolRules() {
    }

    public enum Pool {
        SMALL(
                entry(DinosaurSpecies.DODO, 54),
                entry(DinosaurSpecies.PTERANODON, 32),
                entry(DinosaurSpecies.VELOCIRAPTOR, 14)),
        BIG(
                entry(DinosaurSpecies.PARASAUROLOPHUS, 32),
                entry(DinosaurSpecies.TRICERATOPS, 26),
                entry(DinosaurSpecies.STEGOSAURUS, 22),
                entry(DinosaurSpecies.PTERANODON, 13),
                entry(DinosaurSpecies.VELOCIRAPTOR, 7)),
        LARGE(
                entry(DinosaurSpecies.TYRANNOSAURUS, 32),
                entry(DinosaurSpecies.SPINOSAURUS, 28),
                entry(DinosaurSpecies.STEGOSAURUS, 17),
                entry(DinosaurSpecies.TRICERATOPS, 14),
                entry(DinosaurSpecies.PARASAUROLOPHUS, 9));

        private final WeightedSpecies[] entries;
        private final int totalWeight;

        Pool(WeightedSpecies... entries) {
            this.entries = entries;
            this.totalWeight = Arrays.stream(entries).mapToInt(WeightedSpecies::weight).sum();
        }

        public DinosaurSpecies speciesForRoll(int roll) {
            int remaining = Math.floorMod(roll, totalWeight);
            for (WeightedSpecies entry : entries) {
                remaining -= entry.weight();
                if (remaining < 0) return entry.species();
            }
            return entries[entries.length - 1].species();
        }

        public boolean contains(DinosaurSpecies species) {
            return weightFor(species) > 0;
        }

        public int weightFor(DinosaurSpecies species) {
            return Arrays.stream(entries)
                    .filter(entry -> entry.species() == species)
                    .mapToInt(WeightedSpecies::weight)
                    .findFirst().orElse(0);
        }

        public int totalWeight() {
            return totalWeight;
        }
    }

    private static WeightedSpecies entry(DinosaurSpecies species, int weight) {
        return new WeightedSpecies(species, weight);
    }

    private record WeightedSpecies(DinosaurSpecies species, int weight) {
    }
}
