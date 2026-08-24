package com.primevalworks.world.egg;

import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Arrays;
import java.util.Optional;

public record DinosaurEggGenome(
        DinosaurSpecies species,
        Origin origin,
        int quality,
        int mutationMask,
        int hueVariant
) {
    private static final int SCHEMA_VERSION = 1;
    private static final String ROOT = "PrimevalEggGenome";

    public DinosaurEggGenome {
        quality = Mth.clamp(quality, 0, 100);
        mutationMask &= FieldDodoEntity.MUTATION_HUGE | FieldDodoEntity.MUTATION_ALBINO;
        hueVariant = Mth.clamp(hueVariant, -8, 8);
    }

    public ItemStack writeTo(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag genome = new CompoundTag();
            genome.putInt("Schema", SCHEMA_VERSION);
            genome.putString("Species", species.registryName());
            genome.putString("Origin", origin.serializedName);
            genome.putInt("Quality", quality);
            genome.putInt("MutationMask", mutationMask);
            genome.putInt("HueVariant", hueVariant);
            root.put(ROOT, genome);
        });
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                origin == Origin.BRED
                        ? "item.primevalworks.bred_dinosaur_egg"
                        : "item.primevalworks.incubated_dinosaur_egg",
                Component.translatable("entity.primevalworks." + species.registryName())
        ));
        return stack;
    }

    public static Optional<DinosaurEggGenome> read(ItemStack stack) {
        if (DinosaurEggSize.fromItem(stack).isEmpty()) return Optional.empty();
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag genome = root.getCompound(ROOT).orElse(null);
        if (genome == null || genome.getIntOr("Schema", 0) != SCHEMA_VERSION) return Optional.empty();
        String speciesName = genome.getStringOr("Species", "");
        DinosaurSpecies species = Arrays.stream(DinosaurSpecies.values())
                .filter(candidate -> candidate.registryName().equals(speciesName))
                .findFirst().orElse(null);
        Origin origin = Origin.fromSerializedName(genome.getStringOr("Origin", ""));
        if (species == null || origin == null) return Optional.empty();
        DinosaurEggSize size = DinosaurEggSize.fromItem(stack).orElseThrow();
        if (!size.contains(species)) return Optional.empty();
        return Optional.of(new DinosaurEggGenome(
                species,
                origin,
                genome.getIntOr("Quality", 0),
                genome.getIntOr("MutationMask", 0),
                genome.getIntOr("HueVariant", 0)
        ));
    }

    public enum Origin {
        BRED("bred"),
        INCUBATED("incubated");

        private final String serializedName;

        Origin(String serializedName) {
            this.serializedName = serializedName;
        }

        private static Origin fromSerializedName(String value) {
            return Arrays.stream(values())
                    .filter(origin -> origin.serializedName.equals(value))
                    .findFirst().orElse(null);
        }
    }
}
