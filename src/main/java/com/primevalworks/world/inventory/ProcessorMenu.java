package com.primevalworks.world.inventory;

import com.primevalworks.registry.ModMenus;
import com.primevalworks.world.block.entity.ProcessorBlockEntity;
import com.primevalworks.world.processor.ProcessorRecipes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ProcessorMenu extends AbstractContainerMenu {
    public static final int PROCESSOR_SLOTS = 4;
    public static final int MACHINE_LIFT = 54;
    public static final int INPUT_X = 144;
    public static final int INPUT_Y = 111 - MACHINE_LIFT;
    public static final int OUTPUT_X = 206;
    public static final int OUTPUT_Y = 123 - MACHINE_LIFT;
    public static final int FUEL_X = 254;
    public static final int FUEL_Y = 96 - MACHINE_LIFT;
    public static final int CATALYST_X = 254;
    public static final int CATALYST_Y = 123 - MACHINE_LIFT;
    public static final int PLAYER_INVENTORY_X = 134;
    public static final int PLAYER_INVENTORY_Y = 121;
    public static final int PLAYER_HOTBAR_Y = 179;

    private final Container container;
    private final ContainerData data;

    public ProcessorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(PROCESSOR_SLOTS), new SimpleContainerData(7));
    }

    public ProcessorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.PROCESSOR.get(), containerId);
        checkContainerSize(container, PROCESSOR_SLOTS);
        checkContainerDataCount(data, 7);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);

        addSlot(new Slot(container, ProcessorBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return ProcessorRecipes.isInput(stack); }
        });
        addSlot(new Slot(container, ProcessorBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return inventory.player.level().fuelValues().isFuel(stack);
            }
        });
        addSlot(new Slot(container, ProcessorBlockEntity.CATALYST_SLOT, CATALYST_X, CATALYST_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return ProcessorRecipes.isCatalyst(stack); }
        });
        addSlot(new Slot(container, ProcessorBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

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

    public int burnTime() { return data.get(0); }
    public int burnDuration() { return data.get(1); }
    public int processProgress() { return data.get(2); }
    public int processDuration() { return data.get(3); }
    public boolean hasEnergy() { return data.get(6) != 0; }

    public boolean automationInsertEnabled(int slot) {
        return (data.get(4) & (1 << slot)) != 0;
    }

    public boolean automationExtractEnabled() {
        return (data.get(5) & (1 << ProcessorBlockEntity.OUTPUT_SLOT)) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(container instanceof AutomationConfigurableContainer configurable)) return false;
        if (id >= 0 && id <= 2) {
            configurable.toggleAutomationInsert(id);
            return true;
        }
        if (id == 3) {
            configurable.toggleAutomationExtract(ProcessorBlockEntity.OUTPUT_SLOT);
            return true;
        }
        return false;
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
        if (slotIndex < PROCESSOR_SLOTS) {
            if (!moveItemStackTo(stack, PROCESSOR_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ProcessorRecipes.forInput(container.getItem(ProcessorBlockEntity.INPUT_SLOT))
                .filter(recipe -> stack.is(recipe.catalyst())).isPresent()) {
            if (!moveItemStackTo(stack, ProcessorBlockEntity.CATALYST_SLOT, ProcessorBlockEntity.CATALYST_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (ProcessorRecipes.isInput(stack)) {
            if (!moveItemStackTo(stack, ProcessorBlockEntity.INPUT_SLOT, ProcessorBlockEntity.INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (ProcessorRecipes.isCatalyst(stack)) {
            if (!moveItemStackTo(stack, ProcessorBlockEntity.CATALYST_SLOT, ProcessorBlockEntity.CATALYST_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (player.level().fuelValues().isFuel(stack)) {
            if (!moveItemStackTo(stack, ProcessorBlockEntity.FUEL_SLOT, ProcessorBlockEntity.FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else {
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
