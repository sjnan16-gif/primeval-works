package com.primevalworks.world.block;

import com.primevalworks.world.base.BaseEnergyRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

public final class PoweredPistonBlock extends PistonBaseBlock {
    public PoweredPistonBlock(boolean sticky, BlockBehaviour.Properties properties) {
        super(sticky, properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (BaseEnergyRules.isPowered(level, pos)) super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean movedByPiston
    ) {
        if (BaseEnergyRules.isPowered(level, pos)) {
            super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (BaseEnergyRules.isPowered(level, pos)) super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        return BaseEnergyRules.isPowered(level, pos) && super.triggerEvent(state, level, pos, id, param);
    }
}
