package com.primevalworks.world.inventory;

import com.primevalworks.registry.ModMenus;
import com.primevalworks.registry.ModItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class FoodBoxMenu extends AbstractContainerMenu {
    public static final int FOOD_SLOTS = 10;
    private final Container container;

    public FoodBoxMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(FOOD_SLOTS));
    }

    public FoodBoxMenu(int containerId, Inventory inventory, Container container) {
        super(ModMenus.FOOD_BOX.get(), containerId);
        checkContainerSize(container, FOOD_SLOTS);
        this.container = container;
        container.startOpen(inventory.player);

        for (int index = 0; index < FOOD_SLOTS; index++) {
            addSlot(new Slot(container, index, 6 + index * 20, 16) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isDinosaurFood(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventoryIndex = column + row * 9 + 9;
                addSlot(new FoodInventorySlot(inventory, inventoryIndex,
                        16 + column * 20, 41 + row * 20));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new FoodInventorySlot(inventory, column, 16 + column * 20, 106));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < FOOD_SLOTS) {
            if (!moveItemStackTo(stack, FOOD_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!isDinosaurFood(stack) || !moveItemStackTo(stack, 0, FOOD_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    public static boolean isDinosaurFood(ItemStack stack) {
        return stack.is(ModItemTags.HERBIVORE_FOOD)
                || stack.is(ModItemTags.CARNIVORE_FOOD)
                || stack.is(ModItemTags.FIELD_DODO_FOOD);
    }

    private static final class FoodInventorySlot extends Slot {
        private FoodInventorySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !hasItem() || isDinosaurFood(getItem());
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isDinosaurFood(stack);
        }
    }
}
