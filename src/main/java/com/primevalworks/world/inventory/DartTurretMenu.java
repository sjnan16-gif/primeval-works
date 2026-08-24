package com.primevalworks.world.inventory;

import com.primevalworks.registry.ModItems;
import com.primevalworks.registry.ModMenus;
import com.primevalworks.world.block.entity.DartTurretBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DartTurretMenu extends AbstractContainerMenu {
    public static final int MACHINE_LIFT = 54;
    public static final int MACHINE_SLOTS = DartTurretBlockEntity.AMMO_SLOTS;
    public static final int PLAYER_INVENTORY_X = 134;
    public static final int PLAYER_INVENTORY_Y = 121;
    public static final int PLAYER_HOTBAR_Y = 179;
    private static final int SLOT_X = 185;
    private static final int SLOT_Y = 94;
    private static final int SLOT_STRIDE = 20;
    private final Container container;

    public DartTurretMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS));
    }

    public DartTurretMenu(int containerId, Inventory inventory, Container container) {
        super(ModMenus.DART_TURRET.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        this.container = container;
        container.startOpen(inventory.player);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(container, row * 3 + column,
                        SLOT_X + column * SLOT_STRIDE, SLOT_Y + row * SLOT_STRIDE - MACHINE_LIFT) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(ModItems.DART.get());
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_INVENTORY_X + column * 18, PLAYER_HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.DART.get())) {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        } else if (slotIndex < MACHINE_SLOTS + 27) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS + 27, slots.size(), false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 27, false)) {
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
}
