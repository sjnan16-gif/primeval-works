package com.primevalworks.world.inventory;

import com.primevalworks.registry.ModMenus;
import com.primevalworks.world.block.entity.AncientFurnaceBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipePropertySet;

public final class AncientFurnaceMenu extends AbstractContainerMenu {
    public static final int MACHINE_LIFT = 54;
    public static final int INPUT_X = 148;
    public static final int INPUT_Y = 98 - MACHINE_LIFT;
    public static final int OUTPUT_X = 259;
    public static final int OUTPUT_Y = 115 - MACHINE_LIFT;
    public static final int PLAYER_INVENTORY_X = 134;
    public static final int PLAYER_INVENTORY_Y = 121;
    public static final int PLAYER_HOTBAR_Y = 179;
    public static final int MACHINE_SLOTS = 2;
    private static final int THROTTLE_BUTTON_BASE = 1000;

    private final Container container;
    private final ContainerData data;
    private final RecipePropertySet acceptedInputs;

    public AncientFurnaceMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(6));
    }

    public AncientFurnaceMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.ANCIENT_FURNACE.get(), containerId);
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 6);
        this.container = container;
        this.data = data;
        this.acceptedInputs = inventory.player.level().recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT);
        container.startOpen(inventory.player);

        addSlot(new Slot(container, AncientFurnaceBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return acceptedInputs.test(stack); }
        });
        addSlot(new FurnaceResultSlot(inventory.player, container, AncientFurnaceBlockEntity.OUTPUT_SLOT,
                OUTPUT_X, OUTPUT_Y));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_INVENTORY_X + column * 18, PLAYER_HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public int processProgress() { return data.get(0); }
    public int processDuration() { return data.get(1); }
    public float throttle() { return data.get(2) / 1000.0F; }
    public boolean hasEnergy() { return data.get(3) != 0; }
    public float energyPerSecond() { return data.get(4) / 100.0F; }
    public float speedMultiplier() { return data.get(5) / 100.0F; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(container instanceof AncientFurnaceBlockEntity furnace)
                || id < THROTTLE_BUTTON_BASE || id > THROTTLE_BUTTON_BASE + 1000) return false;
        furnace.setThrottle((id - THROTTLE_BUTTON_BASE) / 1000.0F);
        return true;
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
        } else if (acceptedInputs.test(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
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
