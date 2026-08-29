package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class CommandTableBlock extends BaseEntityBlock {
    public static final MapCodec<CommandTableBlock> CODEC = simpleCodec(CommandTableBlock::new);
    public static final String OWNER_TABLE_POS = "PrimevalCommandTablePos";
    public static final String OWNER_TABLE_DIMENSION = "PrimevalCommandTableDimension";
    public static final int MINIMUM_TABLE_SPACING = 72;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public CommandTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CommandTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return level.isClientSide() ? null : createTickerHelper(
                type,
                com.primevalworks.registry.ModBlockEntities.COMMAND_TABLE.get(),
                CommandTableBlockEntity::serverTick
        );
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel) || !(placer instanceof ServerPlayer player)) {
            if (!level.isClientSide()) {
                level.destroyBlock(pos, false);
            }
            return;
        }

        ExistingTable existing = existingTable(player);
        if (existing != null && !existing.matches(serverLevel, pos)) {
            rejectPlacement(serverLevel, pos, player, "You already control a Command Table at " + existing.positionText() + ".");
            return;
        }

        BlockPos nearby = findNearbyTable(serverLevel, pos);
        if (nearby != null) {
            rejectPlacement(serverLevel, pos, player, "Another base is too close. Command Tables need " + MINIMUM_TABLE_SPACING + " blocks between them.");
            return;
        }

        CompoundTag data = player.getPersistentData();
        data.putLong(OWNER_TABLE_POS, pos.asLong());
        data.putString(OWNER_TABLE_DIMENSION, serverLevel.dimension().identifier().toString());
        if (serverLevel.getBlockEntity(pos) instanceof CommandTableBlockEntity table) {
            table.claim(player.getUUID());
        }
        removeLegacyExtensions(serverLevel, pos);
        DinosaurOwnership.restoreActiveForTable(player, pos);
        player.sendOverlayMessage(Component.literal("Base claimed. This is your only Command Table."));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos extensionPos = extensionPos(pos, state);
        if (level.getBlockState(extensionPos).is(ModBlocks.COMMAND_TABLE_EXTENSION.get())) {
            level.setBlock(extensionPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            releaseClaim(serverPlayer, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    public BlockPos extensionPos(BlockPos masterPos, BlockState state) {
        return masterPos.relative(state.getValue(FACING).getClockWise());
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        removeLegacyExtensions(level, pos);
    }

    public static void scheduleLegacyCleanup(ServerLevel level, BlockPos pos) {
        level.scheduleTick(pos, ModBlocks.COMMAND_TABLE.get(), 1);
    }

    private static void removeLegacyExtensions(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = pos.relative(direction);
            if (level.getBlockState(adjacent).is(ModBlocks.COMMAND_TABLE_EXTENSION.get())) {
                level.setBlock(adjacent, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public static void releaseClaim(ServerPlayer player, BlockPos pos) {
        CompoundTag data = player.getPersistentData();
        if (data.getLongOr(OWNER_TABLE_POS, Long.MIN_VALUE) == pos.asLong()
                && data.getStringOr(OWNER_TABLE_DIMENSION, "").equals(player.level().dimension().identifier().toString())) {
            clearClaim(data);
            player.sendOverlayMessage(Component.literal("Command Table removed. You may claim a new base."));
        }
    }

    public static void copyClaim(Player original, Player replacement) {
        CompoundTag from = original.getPersistentData();
        if (!from.contains(OWNER_TABLE_POS)) {
            return;
        }
        CompoundTag to = replacement.getPersistentData();
        to.putLong(OWNER_TABLE_POS, from.getLongOr(OWNER_TABLE_POS, 0L));
        to.putString(OWNER_TABLE_DIMENSION, from.getStringOr(OWNER_TABLE_DIMENSION, "minecraft:overworld"));
    }

    public static void claimExisting(ServerPlayer player, BlockPos pos) {
        ExistingTable existing = existingTable(player);
        if (existing != null && !existing.matches(player.level(), pos)) {
            player.sendOverlayMessage(Component.literal("You already control a different Command Table at " + existing.positionText() + "."));
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof CommandTableBlockEntity table
                && !table.claim(player.getUUID())) {
            player.sendOverlayMessage(Component.literal("This Command Table already answers to another keeper."));
            return;
        }
        CompoundTag data = player.getPersistentData();
        data.putLong(OWNER_TABLE_POS, pos.asLong());
        data.putString(OWNER_TABLE_DIMENSION, player.level().dimension().identifier().toString());
        DinosaurOwnership.restoreActiveForTable(player, pos);
    }

    public static Optional<ClaimedTable> getClaimedTable(ServerPlayer player) {
        ExistingTable existing = existingTable(player);
        if (existing == null) {
            return Optional.empty();
        }
        Identifier dimensionId = Identifier.tryParse(existing.dimension());
        if (dimensionId == null) {
            return Optional.empty();
        }
        ServerLevel level = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        return level == null ? Optional.empty() : Optional.of(new ClaimedTable(level, existing.pos()));
    }

    private static ExistingTable existingTable(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(OWNER_TABLE_POS) || !data.contains(OWNER_TABLE_DIMENSION)) {
            return null;
        }
        Identifier dimensionId = Identifier.tryParse(data.getStringOr(OWNER_TABLE_DIMENSION, ""));
        if (dimensionId == null) {
            clearClaim(data);
            return null;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel claimedLevel = player.level().getServer().getLevel(dimension);
        BlockPos claimedPos = BlockPos.of(data.getLongOr(OWNER_TABLE_POS, 0L));
        if (claimedLevel == null || !claimedLevel.getBlockState(claimedPos).is(ModBlocks.COMMAND_TABLE.get())) {
            clearClaim(data);
            return null;
        }
        return new ExistingTable(dimensionId.toString(), claimedPos);
    }

    private BlockPos findNearbyTable(ServerLevel level, BlockPos placedPos) {
        int radius = MINIMUM_TABLE_SPACING;
        int radiusSquared = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z >= radiusSquared) {
                    continue;
                }
                for (int y = -32; y <= 32; y++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos candidate = placedPos.offset(x, y, z);
                    if (level.hasChunkAt(candidate) && level.getBlockState(candidate).is(ModBlocks.COMMAND_TABLE.get())) {
                        return candidate.immutable();
                    }
                }
            }
        }
        return null;
    }

    private void rejectPlacement(ServerLevel level, BlockPos pos, ServerPlayer player, String reason) {
        level.destroyBlock(pos, false);
        if (!player.getAbilities().instabuild) {
            popResource(level, pos, new ItemStack(this));
        }
        player.sendOverlayMessage(Component.literal(reason));
    }

    private static void clearClaim(CompoundTag data) {
        data.remove(OWNER_TABLE_POS);
        data.remove(OWNER_TABLE_DIMENSION);
    }

    public static int baseRadius(Level level, BlockPos tablePos) {
        return level.getBlockEntity(tablePos) instanceof CommandTableBlockEntity table
                ? table.baseRadius()
                : 50;
    }

    public static float workDurationMultiplier(Level level, BlockPos tablePos, int jobIndex) {
        return level.getBlockEntity(tablePos) instanceof CommandTableBlockEntity table
                ? table.workDurationMultiplier(jobIndex)
                : 1.0F;
    }

    public static @Nullable CommandTableBlockEntity tableEntity(Level level, BlockPos tablePos) {
        return level.getBlockEntity(tablePos) instanceof CommandTableBlockEntity table ? table : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private record ExistingTable(String dimension, BlockPos pos) {
        private boolean matches(ServerLevel level, BlockPos candidate) {
            return dimension.equals(level.dimension().identifier().toString()) && pos.equals(candidate);
        }

        private String positionText() {
            return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
    }

    public record ClaimedTable(ServerLevel level, BlockPos pos) {
        public ClaimedTable {
            pos = pos.immutable();
        }
    }
}
