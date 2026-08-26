package com.primevalworks.world.block.entity;

import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.AncientBarrelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class AncientBarrelBlockEntity extends BaseContainerBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.primevalworks.ancient_barrel");
    private NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
            setOpen(state, true);
        }

        @Override
        protected void onClose(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
            setOpen(state, false);
        }

        @Override
        protected void openerCountChanged(net.minecraft.world.level.Level level, BlockPos pos,
                                          BlockState state, int previous, int current) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == AncientBarrelBlockEntity.this;
        }
    };

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

    @Override
    public void startOpen(ContainerUser user) {
        if (!remove && !user.getLivingEntity().isSpectator() && level != null) {
            openers.incrementOpeners(user.getLivingEntity(), level, worldPosition, getBlockState(),
                    user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!remove && !user.getLivingEntity().isSpectator() && level != null) {
            openers.decrementOpeners(user.getLivingEntity(), level, worldPosition, getBlockState());
        }
    }

    public void recheckOpen() {
        if (!remove && level != null) {
            openers.recheckOpeners(level, worldPosition, getBlockState());
        }
    }

    private void setOpen(BlockState state, boolean open) {
        if (level != null && state.hasProperty(AncientBarrelBlock.OPEN)) {
            level.setBlock(worldPosition, state.setValue(AncientBarrelBlock.OPEN, open), 3);
        }
    }
}
