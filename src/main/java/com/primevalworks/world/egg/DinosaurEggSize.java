package com.primevalworks.world.egg;

import com.primevalworks.registry.ModBlocks;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.entity.DinosaurSpecies;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public enum DinosaurEggSize {
    SMALL(PremiumIncubationRules.SMALL_BASE_TICKS, EggFragmentRules.SMALL, DinosaurEggPoolRules.Pool.SMALL),
    BIG(PremiumIncubationRules.BIG_BASE_TICKS, EggFragmentRules.BIG, DinosaurEggPoolRules.Pool.BIG),
    LARGE(PremiumIncubationRules.LARGE_BASE_TICKS, EggFragmentRules.LARGE, DinosaurEggPoolRules.Pool.LARGE);

    private final int baseIncubationTicks;
    private final EggFragmentRules fragmentRules;
    private final DinosaurEggPoolRules.Pool speciesPool;

    DinosaurEggSize(
            int baseIncubationTicks,
            EggFragmentRules fragmentRules,
            DinosaurEggPoolRules.Pool speciesPool
    ) {
        this.baseIncubationTicks = baseIncubationTicks;
        this.fragmentRules = fragmentRules;
        this.speciesPool = speciesPool;
    }

    public int baseIncubationTicks() {
        return baseIncubationTicks;
    }

    public DinosaurSpecies randomSpecies(RandomSource random) {
        return speciesPool.speciesForRoll(random.nextInt(speciesPool.totalWeight()));
    }

    public int fossilFragmentCount(RandomSource random) {
        return fragmentRules.count(random.nextFloat(), random.nextFloat());
    }

    public boolean contains(DinosaurSpecies candidate) {
        return speciesPool.contains(candidate);
    }

    public int weightFor(DinosaurSpecies candidate) {
        return speciesPool.weightFor(candidate);
    }

    public int totalWeight() {
        return speciesPool.totalWeight();
    }

    public Item item() {
        return switch (this) {
            case SMALL -> ModItems.SMALL_DINOSAUR_EGG.get();
            case BIG -> ModItems.BIG_DINOSAUR_EGG.get();
            case LARGE -> ModItems.LARGE_DINOSAUR_EGG.get();
        };
    }

    public Block block() {
        return switch (this) {
            case SMALL -> ModBlocks.SMALL_DINOSAUR_EGG.get();
            case BIG -> ModBlocks.BIG_DINOSAUR_EGG.get();
            case LARGE -> ModBlocks.LARGE_DINOSAUR_EGG.get();
        };
    }

    public static Optional<DinosaurEggSize> fromItem(ItemStack stack) {
        for (DinosaurEggSize size : values()) {
            if (stack.is(size.item())) {
                return Optional.of(size);
            }
        }
        return Optional.empty();
    }

    public static DinosaurEggSize forSpecies(DinosaurSpecies species) {
        for (DinosaurEggSize size : values()) {
            if (size.contains(species)) return size;
        }
        throw new IllegalArgumentException("No egg size contains species " + species.registryName());
    }

    public String translationKey() {
        return "egg_size.primevalworks." + name().toLowerCase();
    }

}
