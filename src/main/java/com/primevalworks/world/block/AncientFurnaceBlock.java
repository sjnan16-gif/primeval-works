package com.primevalworks.world.block;

import com.primevalworks.world.sound.PrimevalSoundPlayback;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.entity.AncientFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class AncientFurnaceBlock extends AbstractFurnaceBlock {
    public static final MapCodec<AncientFurnaceBlock> CODEC = simpleCodec(AncientFurnaceBlock::new);

    public AncientFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends AbstractFurnaceBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AncientFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return level.isClientSide() ? null : createTickerHelper(
                type,
                ModBlockEntities.ANCIENT_FURNACE.get(),
                (tickLevel, pos, tickState, furnace) -> AncientFurnaceBlockEntity.serverTick(
                        (net.minecraft.server.level.ServerLevel) tickLevel,
                        pos,
                        tickState,
                        furnace
                )
        );
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof AncientFurnaceBlockEntity furnace) {
            player.openMenu((MenuProvider)furnace);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.32D;
        double z = pos.getZ() + 0.5D;
        double side = random.nextDouble() * 0.46D - 0.23D;
        double dx = facing.getAxis() == Direction.Axis.X ? facing.getStepX() * 0.52D : side;
        double dz = facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() * 0.52D : side;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y, z + dz, 0.0D, 0.0D, 0.0D);
        if (random.nextDouble() < 0.18D) {
            PrimevalSoundPlayback.playLocalAt(level, x, y, z, SoundEvents.BLASTFURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS, 0.52F, 0.92F + random.nextFloat() * 0.12F,
                    PrimevalSoundPlayback.MACHINE_RADIUS);
        }
    }
}
