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
    SMALL(400, new DinosaurSpecies[]{
            DinosaurSpecies.DODO,
            DinosaurSpecies.VELOCIRAPTOR,
            DinosaurSpecies.PTERANODON
    }),
    BIG(800, new DinosaurSpecies[]{
            DinosaurSpecies.PARASAUROLOPHUS,
            DinosaurSpecies.STEGOSAURUS,
            DinosaurSpecies.TRICERATOPS
    }),
    LARGE(1200, new DinosaurSpecies[]{
            DinosaurSpecies.TYRANNOSAURUS,
            DinosaurSpecies.SPINOSAURUS
    });

    private final int baseIncubationTicks;
    private final DinosaurSpecies[] species;

    DinosaurEggSize(int baseIncubationTicks, DinosaurSpecies[] species) {
        this.baseIncubationTicks = baseIncubationTicks;
        this.species = species;
    }

    public int baseIncubationTicks() {
        return baseIncubationTicks;
    }

    public DinosaurSpecies randomSpecies(RandomSource random) {
        return species[random.nextInt(species.length)];
    }

    public boolean contains(DinosaurSpecies candidate) {
        for (DinosaurSpecies member : species) {
            if (member == candidate) {
                return true;
            }
        }
        return false;
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
