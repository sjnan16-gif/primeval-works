package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModSounds;
import com.primevalworks.world.egg.DinosaurEggSize;
import com.primevalworks.world.egg.DinosaurHatching;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class DinosaurEggBlock extends Block {
    private final DinosaurEggSize size;
    private final VoxelShape shape;

    protected DinosaurEggBlock(BlockBehaviour.Properties properties, DinosaurEggSize size, VoxelShape shape) {
        super(properties);
        this.size = size;
        this.shape = shape;
    }

    public DinosaurEggSize size() {
        return size;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP, SupportType.CENTER);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        DinosaurHatching.HatchResult result = DinosaurHatching.hatchWildEgg(serverPlayer, size);
        if (!result.success()) {
            serverPlayer.sendOverlayMessage(result.message());
            return InteractionResult.FAIL;
        }
        level.removeBlock(pos, false);
        // A first wild hatch supplies the fossil needed to establish a Command Table,
        // keeping the opening progression free of an expedition-before-base loop.
        popResource(level, pos, new ItemStack(com.primevalworks.registry.ModItems.FOSSIL_FRAGMENT.get()));
        if (ModSounds.areAssetsReady()) {
            level.playSound(null, pos, ModSounds.EGG_HATCH.get(), SoundSource.BLOCKS, 0.85F, 1.0F);
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                    pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D,
                    12, 0.25D, 0.2D, 0.25D, 0.02D);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool
    ) {
        if (!level.isClientSide()
                && !player.getAbilities().instabuild
                && EnchantmentHelper.getItemEnchantmentLevel(
                        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
                        tool
                ) > 0) {
            popResource(level, pos, new ItemStack(size.item()));
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    public static final class Small extends DinosaurEggBlock {
        public static final MapCodec<Small> CODEC = simpleCodec(Small::new);

        public Small(BlockBehaviour.Properties properties) {
            super(properties, DinosaurEggSize.SMALL, box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }
    }

    public static final class Big extends DinosaurEggBlock {
        public static final MapCodec<Big> CODEC = simpleCodec(Big::new);

        public Big(BlockBehaviour.Properties properties) {
            super(properties, DinosaurEggSize.BIG, box(2.0D, 0.0D, 2.0D, 14.0D, 14.0D, 14.0D));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }
    }

    public static final class Large extends DinosaurEggBlock {
        public static final MapCodec<Large> CODEC = simpleCodec(Large::new);

        public Large(BlockBehaviour.Properties properties) {
            super(properties, DinosaurEggSize.LARGE, box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }
    }
}
