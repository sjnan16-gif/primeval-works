package com.primevalworks.world.block;

import com.primevalworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class HeavyPistonRules {
    private HeavyPistonRules() {
    }

    public static boolean isReinforced(Block block) {
        return block == ModBlocks.REINFORCED_PISTON.get()
                || block == ModBlocks.STICKY_REINFORCED_PISTON.get();
    }

    public static boolean isStickyReinforced(Block block) {
        return block == ModBlocks.STICKY_REINFORCED_PISTON.get();
    }

    public static boolean isReinforcedPistonAt(Level level, BlockPos pistonPos) {
        BlockState pistonState = level.getBlockState(pistonPos);
        if (isReinforced(pistonState.getBlock())) return true;
        if (!pistonState.is(Blocks.MOVING_PISTON)) return false;
        BlockEntity blockEntity = level.getBlockEntity(pistonPos);
        return blockEntity instanceof PistonMovingBlockEntity moving
                && isReinforced(moving.getMovedState().getBlock());
    }

    public static boolean canMoveObsidian(
            BlockState state, Level level, BlockPos pos, Direction direction
    ) {
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.CRYING_OBSIDIAN)) return false;
        if (!level.getWorldBorder().isWithinBounds(pos)) return false;
        if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY()) return false;
        if (direction == Direction.DOWN && pos.getY() == level.getMinY()) return false;
        if (direction == Direction.UP && pos.getY() == level.getMaxY()) return false;
        return !state.hasBlockEntity();
    }
}
