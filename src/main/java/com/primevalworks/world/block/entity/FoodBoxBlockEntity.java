package com.primevalworks.world.block.entity;

import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.FoodBoxBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.primevalworks.world.inventory.FoodBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FoodBoxBlockEntity extends BaseContainerBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.primevalworks.food_box");
    private NonNullList<ItemStack> items = NonNullList.withSize(FoodBoxMenu.FOOD_SLOTS, ItemStack.EMPTY);

    public FoodBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOOD_BOX.get(), pos, state);
    }

    @Override
    public void onLoad() {
        syncFullState();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        syncFullState();
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = super.removeItemNoUpdate(slot);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        setChanged();
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

    @Override
    public int getContainerSize() {
        return FoodBoxMenu.FOOD_SLOTS;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return FoodBoxMenu.isDinosaurFood(stack);
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FoodBoxMenu(containerId, inventory, this);
    }

    private void syncFullState() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(FoodBoxBlock.FULL)) return;
        boolean full = !isEmpty();
        if (state.getValue(FoodBoxBlock.FULL) != full) {
            level.setBlock(worldPosition, state.setValue(FoodBoxBlock.FULL, full), Block.UPDATE_CLIENTS);
        }
    }
}
