package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.entity.MagicTurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class MagicTurretBlock extends BaseEntityBlock {
    public static final MapCodec<MagicTurretBlock> CODEC = simpleCodec(MagicTurretBlock::new);

    public MagicTurretBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicTurretBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MAGIC_TURRET.get(), MagicTurretBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.MAGIC_TURRET.get(),
                        (tickLevel, pos, tickState, turret) -> MagicTurretBlockEntity.serverTick(
                                (net.minecraft.server.level.ServerLevel)tickLevel, pos, tickState, turret));
    }
}
