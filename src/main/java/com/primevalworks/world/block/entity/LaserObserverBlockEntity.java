package com.primevalworks.world.block.entity;

import com.primevalworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class LaserObserverBlockEntity extends BlockEntity {
    public LaserObserverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_OBSERVER.get(), pos, state);
    }
}
