package com.primevalworks.world.processor;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

public final class ProcessorRecipes {
    private static final List<ProcessorRecipe> RECIPES = List.of(
            recipe("ancient_metal", ModItems.RAW_ANCIENT_METAL_INGOT.get(), ModItems.SULFUR.get(),
                    ModItems.ANCIENT_METAL_INGOT.get(), 200),
            recipe("ancient_spell", ModItems.RAW_ANCIENT_SPELL_INGOT.get(), ModItems.FOSSIL_FRAGMENT.get(),
                    ModItems.ANCIENT_SPELL_INGOT.get(), 260),
            recipe("compressed_metal", ModItems.ANCIENT_METAL_INGOT.get(), ModItems.CORE.get(),
                    ModItems.COMPRESSED_ANCIENT_METAL_INGOT.get(), 320),
            recipe("compressed_core", ModItems.CORE.get(), ModItems.ANCIENT_METAL_INGOT.get(),
                    ModItems.COMPRESSED_CORE.get(), 360),
            recipe("reinforced_piston", Items.PISTON, ModItems.ANCIENT_METAL_INGOT.get(),
                    ModItems.REINFORCED_PISTON.get(), 260),
            recipe("sticky_reinforced_piston", ModItems.REINFORCED_PISTON.get(), Items.SLIME_BALL,
                    ModItems.STICKY_REINFORCED_PISTON.get(), 180),
            recipe("ancient_furnace", Items.BLAST_FURNACE, ModItems.COMPRESSED_ANCIENT_METAL_INGOT.get(),
                    ModItems.ANCIENT_FURNACE.get(), 360),
            recipe("enhanced_rail", Items.POWERED_RAIL, ModItems.ANCIENT_METAL_INGOT.get(),
                    ModItems.ENHANCED_RAIL.get(), 180)
    );

    private ProcessorRecipes() {
    }

    public static List<ProcessorRecipe> all() {
        return RECIPES;
    }

    public static Optional<ProcessorRecipe> find(ItemStack input, ItemStack catalyst) {
        return RECIPES.stream().filter(recipe -> recipe.matches(input, catalyst)).findFirst();
    }

    public static Optional<ProcessorRecipe> forInput(ItemStack input) {
        return RECIPES.stream().filter(recipe -> input.is(recipe.input())).findFirst();
    }

    public static boolean isInput(ItemStack stack) {
        return RECIPES.stream().anyMatch(recipe -> stack.is(recipe.input()));
    }

    public static boolean isCatalyst(ItemStack stack) {
        return RECIPES.stream().anyMatch(recipe -> stack.is(recipe.catalyst()));
    }

    private static ProcessorRecipe recipe(String path, Item input, Item catalyst, Item output, int ticks) {
        return new ProcessorRecipe(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, path), input, catalyst, output, ticks
        );
    }
}
