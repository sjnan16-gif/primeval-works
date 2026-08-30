package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.world.egg.DinosaurEggSize;
import com.primevalworks.world.egg.DinosaurEggGenome;
import com.primevalworks.world.egg.DinosaurHatching;
import com.primevalworks.world.block.entity.DinosaurEggBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class DinosaurEggBlock extends BaseEntityBlock {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.primevalworks.world.block.entity.DinosaurEggBlockEntity(pos, state);
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
        DinosaurEggBlockEntity eggBlockEntity = level.getBlockEntity(pos) instanceof DinosaurEggBlockEntity egg
                ? egg : null;
        DinosaurEggGenome genome = eggBlockEntity == null
                ? null : DinosaurEggGenome.read(eggBlockEntity.geneticEgg()).orElse(null);
        DinosaurHatching.HatchResult result = genome == null
                ? DinosaurHatching.hatchWildEgg(serverPlayer, size)
                : DinosaurHatching.hatchForPlayer(serverPlayer, genome.hatchingGenome());
        if (!result.success()) {
            serverPlayer.sendOverlayMessage(result.message());
            return InteractionResult.FAIL;
        }
        level.removeBlock(pos, false);
        if (genome == null) {
            popResource(level, pos, new ItemStack(
                    com.primevalworks.registry.ModItems.FOSSIL_FRAGMENT.get(),
                    size.fossilFragmentCount(level.getRandom())
            ));
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
            ItemStack storedEgg = blockEntity instanceof DinosaurEggBlockEntity egg
                    ? egg.geneticEgg() : ItemStack.EMPTY;
            popResource(level, pos, storedEgg.isEmpty() ? new ItemStack(size.item()) : storedEgg);
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    public static final class Small extends DinosaurEggBlock {
        public static final MapCodec<Small> CODEC = simpleCodec(Small::new);

        public Small(BlockBehaviour.Properties properties) {
            super(properties, DinosaurEggSize.SMALL, box(5.0D, 0.0D, 5.0D, 11.0D, 8.0D, 11.0D));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }

    public static final class Big extends DinosaurEggBlock {
        public static final MapCodec<Big> CODEC = simpleCodec(Big::new);

        public Big(BlockBehaviour.Properties properties) {
            super(properties, DinosaurEggSize.BIG, box(3.5D, 0.0D, 3.5D, 12.5D, 12.0D, 12.5D));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }

    public static final class Large extends DinosaurEggBlock {
        public static final MapCodec<Large> CODEC = simpleCodec(Large::new);

        public Large(BlockBehaviour.Properties properties) {
            super(properties, DinosaurEggSize.LARGE, box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }
}
