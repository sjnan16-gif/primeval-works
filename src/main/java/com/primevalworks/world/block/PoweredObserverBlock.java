package com.primevalworks.world.block;

import com.primevalworks.world.base.BaseEnergyRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class PoweredObserverBlock extends ObserverBlock {
    public static final int DETECTION_RANGE = 5;
    private static final DustParticleOptions BEAM_EDGE = new DustParticleOptions(0x8F1822, 0.22F);
    private static final DustParticleOptions BEAM_CORE = new DustParticleOptions(0xFF5961, 0.11F);
    private static final DustParticleOptions BEAM_END = new DustParticleOptions(0xFF202B, 0.34F);

    public PoweredObserverBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static void onDistantBlockUpdate(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        notifyDistantObservers(level, event.getPos());
    }

    public static void notifyDistantObservers(ServerLevel level, BlockPos changed) {
        for (BlockPos observerPos : BaseEnergyRules.poweredConsumers(
                level, com.primevalworks.registry.ModBlocks.LASER_OBSERVER.get())) {
            BlockState observer = level.getBlockState(observerPos);
            if (!observer.hasProperty(FACING) || !observer.hasProperty(POWERED)) continue;
            Direction facing = observer.getValue(FACING);
            boolean onBeam = false;
            for (int distance = 1; distance <= DETECTION_RANGE; distance++) {
                if (observerPos.relative(facing, distance).equals(changed)) {
                    onBeam = true;
                    break;
                }
            }
            if (onBeam && !observer.getValue(POWERED)
                    && !level.getBlockTicks().hasScheduledTick(observerPos, observer.getBlock())) {
                level.scheduleTick(observerPos, observer.getBlock(), 2);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!BaseEnergyRules.isPowered(level, pos)) {
            if (state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_CLIENTS);
                updateNeighborsInFront(level, pos, state);
            }
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level instanceof Level realLevel && BaseEnergyRules.isPowered(realLevel, pos)
                ? super.getSignal(state, level, pos, direction)
                : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level instanceof Level realLevel && BaseEnergyRules.isPowered(realLevel, pos)
                ? super.getDirectSignal(state, level, pos, direction)
                : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        double startX = pos.getX() + 0.5D + facing.getStepX() * 0.52D;
        double startY = pos.getY() + 0.5D + facing.getStepY() * 0.52D;
        double startZ = pos.getZ() + 0.5D + facing.getStepZ() * 0.52D;
        for (int step = 0; step <= DETECTION_RANGE * 3; step++) {
            double distance = step / 3.0D;
            double x = startX + facing.getStepX() * distance;
            double y = startY + facing.getStepY() * distance;
            double z = startZ + facing.getStepZ() * distance;
            level.addParticle(BEAM_EDGE, x, y, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(BEAM_CORE, x, y, z, 0.0D, 0.0D, 0.0D);
        }

        double endX = startX + facing.getStepX() * DETECTION_RANGE;
        double endY = startY + facing.getStepY() * DETECTION_RANGE;
        double endZ = startZ + facing.getStepZ() * DETECTION_RANGE;
        for (int particle = 0; particle < 3; particle++) {
            level.addParticle(BEAM_END,
                    endX + random.triangle(0.0D, 0.055D),
                    endY + random.triangle(0.0D, 0.055D),
                    endZ + random.triangle(0.0D, 0.055D),
                    0.0D, 0.0D, 0.0D);
        }
    }
}
