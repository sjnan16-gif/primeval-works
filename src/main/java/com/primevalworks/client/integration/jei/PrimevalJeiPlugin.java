package com.primevalworks.client.integration.jei;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModItems;
import com.primevalworks.registry.ModMenus;
import com.primevalworks.world.inventory.ProcessorMenu;
import com.primevalworks.world.processor.ProcessorRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;

@JeiPlugin
public final class PrimevalJeiPlugin implements IModPlugin {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ProcessorRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ProcessorRecipeCategory.TYPE, ProcessorRecipes.all());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(ProcessorRecipeCategory.TYPE, ModItems.PROCESSOR.get());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                ProcessorMenu.class, ModMenus.PROCESSOR.get(), ProcessorRecipeCategory.TYPE,
                0, 3, ProcessorMenu.PROCESSOR_SLOTS, 36
        );
    }
}
