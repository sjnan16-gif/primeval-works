package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.entity.LaserTurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class LaserTurretBlock extends BaseEntityBlock {
    public static final MapCodec<LaserTurretBlock> CODEC = simpleCodec(LaserTurretBlock::new);

    public LaserTurretBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserTurretBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.LASER_TURRET.get(), LaserTurretBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.LASER_TURRET.get(),
                        (tickLevel, pos, tickState, turret) -> LaserTurretBlockEntity.serverTick(
                                (net.minecraft.server.level.ServerLevel)tickLevel, pos, tickState, turret));
    }
}
