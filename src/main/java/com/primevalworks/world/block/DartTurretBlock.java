package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.entity.DartTurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class DartTurretBlock extends BaseEntityBlock {
    public static final MapCodec<DartTurretBlock> CODEC = simpleCodec(DartTurretBlock::new);

    public DartTurretBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DartTurretBlockEntity turret) {
            player.openMenu(turret);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                                boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof DartTurretBlockEntity turret) {
            Containers.dropContents(level, pos, turret);
        }
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DartTurretBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.DART_TURRET.get(), DartTurretBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.DART_TURRET.get(),
                        (tickLevel, pos, tickState, turret) -> DartTurretBlockEntity.serverTick(
                                (ServerLevel)tickLevel, pos, tickState, turret));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos,
                                        net.minecraft.core.Direction direction) {
        return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(
                level.getBlockEntity(pos));
    }
}
