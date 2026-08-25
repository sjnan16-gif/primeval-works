package com.primevalworks.world.block;

import com.primevalworks.registry.ModItems;
import com.primevalworks.world.sound.PrimevalSoundPlayback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public final class PrimevalBerryBushBlock extends SweetBerryBushBlock {
    public PrimevalBerryBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ModItems.BERRIES.get().getDefaultInstance();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hitResult) {
        int age = state.getValue(AGE);
        if (age <= 1) return super.useWithoutItem(state, level, pos, player, hitResult);
        if (level instanceof ServerLevel serverLevel) {
            int count = 1 + serverLevel.getRandom().nextInt(2) + (age == MAX_AGE ? 1 : 0);
            Block.popResource(serverLevel, pos, new ItemStack(ModItems.BERRIES.get(), count));
            PrimevalSoundPlayback.playAt(serverLevel, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                    SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F,
                    PrimevalSoundPlayback.SMALL_RADIUS);
            BlockState picked = state.setValue(AGE, 1);
            serverLevel.setBlock(pos, picked, Block.UPDATE_CLIENTS);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        }
        return InteractionResult.SUCCESS;
    }
}
