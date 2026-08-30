package com.primevalworks.registry;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.inventory.FoodBoxMenu;
import com.primevalworks.world.inventory.ProcessorMenu;
import com.primevalworks.world.inventory.AncientFurnaceMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, PrimevalWorks.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FoodBoxMenu>> FOOD_BOX = MENUS.register(
            "food_box",
            () -> new MenuType<>(FoodBoxMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessorMenu>> PROCESSOR = MENUS.register(
            "processor",
            () -> new MenuType<>(ProcessorMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<AncientFurnaceMenu>> ANCIENT_FURNACE = MENUS.register(
            "ancient_furnace",
            () -> new MenuType<>(AncientFurnaceMenu::new, FeatureFlags.VANILLA_SET)
    );
    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
