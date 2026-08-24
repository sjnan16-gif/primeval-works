package com.primevalworks.client.integration.jei;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.processor.ProcessorRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ProcessorRecipeCategory implements IRecipeCategory<ProcessorRecipe> {
    public static final IRecipeType<ProcessorRecipe> TYPE = IRecipeType.create(
            PrimevalWorks.MOD_ID, "processor", ProcessorRecipe.class
    );
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            PrimevalWorks.MOD_ID, "textures/gui/processor_ui.png"
    );
    private final IDrawable icon;
    private final IDrawableStatic machine;
    private final IDrawableStatic chimney;
    private final IDrawableStatic flame;
    private final IDrawable blankSlot;

    public ProcessorRecipeCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.PROCESSOR.get()));
        machine = guiHelper.drawableBuilder(TEXTURE, 111, 82, 195, 80)
                .setTextureSize(427, 240)
                .build();
        chimney = guiHelper.drawableBuilder(TEXTURE, 196, 65, 36, 17)
                .setTextureSize(427, 240)
                .build();
        flame = guiHelper.drawableBuilder(TEXTURE, 235, 61, 44, 17)
                .setTextureSize(427, 240)
                .build();
        blankSlot = guiHelper.createBlankDrawable(18, 18);
    }

    @Override public IRecipeType<ProcessorRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.primevalworks.processor"); }
    @Override public int getWidth() { return 195; }
    @Override public int getHeight() { return 101; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProcessorRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(33, 50)
                .setBackground(blankSlot, 0, 0)
                .add(new ItemStack(recipe.input()))
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.literal("Material")));
        var fuelSlot = builder.addInputSlot(143, 35)
                .setBackground(blankSlot, 0, 0)
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.literal("Furnace fuel")));
        var level = Minecraft.getInstance().level;
        if (level == null) {
            fuelSlot.add(new ItemStack(Items.COAL))
                    .add(new ItemStack(Items.CHARCOAL))
                    .add(new ItemStack(Items.BLAZE_ROD));
        } else {
            level.fuelValues().fuelItems().forEach(item -> fuelSlot.add(new ItemStack(item)));
        }
        builder.addInputSlot(143, 62)
                .setBackground(blankSlot, 0, 0)
                .add(new ItemStack(recipe.catalyst()))
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.literal("Catalyst")));
        builder.addOutputSlot(95, 62)
                .setBackground(blankSlot, 0, 0)
                .add(recipe.outputStack());
    }

    @Override
    public void draw(ProcessorRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics,
                     double mouseX, double mouseY) {
        machine.draw(graphics, 0, 21);
        chimney.draw(graphics, 85, 4);
        flame.draw(graphics, 81, 27);
    }

    @Override public boolean needsRecipeBorder() { return false; }

    @Override
    public Identifier getIdentifier(ProcessorRecipe recipe) {
        return recipe.id();
    }
}
