package com.primevalworks.world.processor;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ProcessorRecipe(
        Identifier id,
        Item input,
        Item catalyst,
        Item output,
        int processTicks
) {
    public boolean matches(ItemStack inputStack, ItemStack catalystStack) {
        return inputStack.is(input) && catalystStack.is(catalyst);
    }

    public ItemStack outputStack() {
        return new ItemStack(output);
    }
}
