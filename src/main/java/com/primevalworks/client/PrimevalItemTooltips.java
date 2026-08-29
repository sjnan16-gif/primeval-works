package com.primevalworks.client;

import com.primevalworks.PrimevalWorks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class PrimevalItemTooltips {
    private PrimevalItemTooltips() {
    }

    public static void add(ItemTooltipEvent event) {
        var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!id.getNamespace().equals(PrimevalWorks.MOD_ID)) return;
        String description = description(id.getPath());
        if (!description.isBlank()) {
            event.getToolTip().add(Component.literal(description).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String description(String path) {
        if (path.endsWith("_spawn_egg")) return "Spawns this dinosaur.";
        return switch (path) {
            case "fossil_fragment" -> "Restores natural pigment.";
            case "dino_whistle" -> "Directs following dinosaurs in the field.";
            case "small_dinosaur_egg" -> "Hatches a small dinosaur.";
            case "big_dinosaur_egg" -> "Hatches a medium dinosaur.";
            case "large_dinosaur_egg" -> "Hatches a large dinosaur.";
            case "command_table" -> "Controls your dinosaur base.";
            case "food_box" -> "Automatically feeds hungry dinosaurs.";
            case "wind_turbine" -> "A basic wind-powered generator.";
            case "upgraded_wind_turbine" -> "Generates wind energy at full output.";
            case "water_turbine" -> "Generates energy in water.";
            case "laser_observer" -> "Detects distant block updates.";
            case "ancient_barrel" -> "High-capacity automation storage.";
            case "dart_turret" -> "Automated base defense.";
            case "processor" -> "Refines ancient materials.";
            case "ancient_furnace" -> "A fast powered furnace.";
            case "ancient_spell_stone" -> "Suppresses nearby hostile spawns.";
            case "laser_turret" -> "Cuts through hostile targets with a concentrated beam.";
            case "spinosaurus_head" -> "A rare trophy used to build the Laser Turret.";
            case "premium_egg_incubator" -> "Improves incubated dinosaurs.";
            case "pteranodon_saddle" -> "Enables Pteranodon flight.";
            case "spinosaurus_saddle" -> "Enables Spinosaurus riding.";
            case "primordial_sword" -> "A balanced ancient blade with a much wider fully charged sweep.";
            case "magic_shard_fragment" -> "Refined into a Magic Shard.";
            case "magic_shard" -> "Concentrated primordial magic.";
            case "hardwood" -> "Strong crafting timber.";
            case "silk" -> "Rare precision material.";
            case "raw_ancient_metal_ingot" -> "Processed into Ancient Metal.";
            case "ancient_metal_ingot" -> "Refined ancient metal.";
            case "ancient_metal_nugget" -> "A small piece of refined ancient metal.";
            case "compressed_ancient_metal_ingot" -> "Dense endgame metal.";
            case "sulfur" -> "Ancient Metal catalyst.";
            case "big_dino_bone" -> "Large dinosaur trophy.";
            case "small_dino_bone" -> "Small dinosaur trophy.";
            case "pteranodon_wing_fragment" -> "Upgrades a Wind Turbine in the Processor.";
            case "tyrannosaurus_tooth" -> "Tyrannosaurus crafting trophy.";
            case "dodo_feather" -> "Light crafting material.";
            case "core" -> "Rare Processor material.";
            case "compressed_core" -> "Advanced power component.";
            case "nesting_treat" -> "Primes matching dinosaurs to breed.";
            case "dart" -> "Dart Turret ammunition.";
            case "berries" -> "Herbivore food and crop.";
            case "big_dino_meat" -> "Food for large carnivores.";
            case "small_dino_meat" -> "Food for small carnivores.";
            case "cooked_dino_meat" -> "Cooked meat from a small dinosaur.";
            case "cooked_large_dino_meat" -> "Cooked meat from a large dinosaur.";
            case "roasted_beet" -> "A filling cooked meal.";
            case "fire_roasted_melon" -> "A light cooked meal.";
            default -> "Primeval crafting material.";
        };
    }
}
