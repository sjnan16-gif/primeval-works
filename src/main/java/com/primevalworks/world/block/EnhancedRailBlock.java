package com.primevalworks.world.block;

import com.primevalworks.world.base.BaseEnergyRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class EnhancedRailBlock extends PoweredRailBlock {
    private static final double BOOST = 1.18D;
    private static final double MAX_HORIZONTAL_SPEED = 1.25D;

    public EnhancedRailBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!BaseEnergyRules.isPowered(level, pos)) {
            if (!level.isClientSide() && state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, false), UPDATE_CLIENTS);
            }
            return;
        }
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide() || !(entity instanceof AbstractMinecart minecart)) {
            return;
        }
        Vec3 movement = minecart.getDeltaMovement();
        double horizontalSpeed = movement.horizontalDistance();
        if (horizontalSpeed < 0.01D || horizontalSpeed >= MAX_HORIZONTAL_SPEED) {
            return;
        }
        double multiplier = Math.min(BOOST, MAX_HORIZONTAL_SPEED / horizontalSpeed);
        minecart.setDeltaMovement(movement.x * multiplier, movement.y, movement.z * multiplier);
    }

    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighbor) {
        if (!BaseEnergyRules.isPowered(level, pos)) {
            if (state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, false), UPDATE_CLIENTS);
            return;
        }
        super.updateState(state, level, pos, neighbor);
    }
}
