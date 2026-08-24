package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.world.block.entity.PremiumEggIncubatorBlockEntity;
import com.primevalworks.world.egg.DinosaurEggSize;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public final class PremiumEggIncubatorBlock extends BaseEntityBlock {
    public static final MapCodec<PremiumEggIncubatorBlock> CODEC = simpleCodec(PremiumEggIncubatorBlock::new);

    public PremiumEggIncubatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (DinosaurEggSize.fromItem(stack).isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PremiumEggIncubatorBlockEntity incubator)) {
            return InteractionResult.FAIL;
        }
        PremiumEggIncubatorBlockEntity.InsertResult result = incubator.insertEgg(serverPlayer, stack);
        serverPlayer.sendOverlayMessage(result.message());
        if (!result.success()) {
            return InteractionResult.FAIL;
        }
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PremiumEggIncubatorBlockEntity incubator)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && incubator.hasEgg()) {
            ItemStack removed = incubator.removeEgg();
            if (!player.addItem(removed)) {
                popResource(level, pos.above(), removed);
            }
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.primevalworks.incubator.removed"));
        } else {
            player.sendOverlayMessage(incubator.statusMessage());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PremiumEggIncubatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PREMIUM_EGG_INCUBATOR.get(), PremiumEggIncubatorBlockEntity::serverTick);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof PremiumEggIncubatorBlockEntity incubator && incubator.hasEgg()) {
            popResource(level, pos, incubator.getEgg().copy());
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
