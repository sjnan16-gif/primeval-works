package com.primevalworks.world.block.entity;

import com.primevalworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class AncientBarrelBlockEntity extends BaseContainerBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.primevalworks.ancient_barrel");
    private NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);

    public AncientBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCIENT_BARREL.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override public int getContainerSize() { return 54; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return DEFAULT_NAME; }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.sixRows(containerId, inventory, this);
    }
}
