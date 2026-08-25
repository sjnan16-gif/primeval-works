package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.entity.TurbineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class TurbineBlock extends BaseEntityBlock {
    public static final MapCodec<TurbineBlock> CODEC = simpleCodec(TurbineBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final List<PartOffset> WIND_PARTS = List.of(
            new PartOffset(-1, 1), new PartOffset(0, 1), new PartOffset(1, 1),
            new PartOffset(-1, 2), new PartOffset(0, 2), new PartOffset(1, 2),
            new PartOffset(0, 3)
    );
    private static final List<PartOffset> WATER_PARTS = List.of(
            new PartOffset(-1, 0), new PartOffset(1, 0),
            new PartOffset(-1, 1), new PartOffset(0, 1), new PartOffset(1, 1)
    );
    private static final VoxelShape WIND_BASE = Shapes.or(
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D),
            Block.box(6.0D, 3.0D, 6.0D, 10.0D, 16.0D, 10.0D)
    );

    public TurbineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        BlockPos masterPos = context.getClickedPos();
        for (PartOffset offset : parts(state)) {
            BlockPos partPos = partPos(masterPos, state, offset);
            if (!context.getLevel().getBlockState(partPos).canBeReplaced()) {
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            assemble(level, pos, state);
        }
    }

    public void assemble(Level level, BlockPos masterPos, BlockState state) {
        boolean wind = state.is(ModBlocks.WIND_TURBINE.get());
        Direction facing = state.getValue(FACING);
        for (PartOffset offset : parts(state)) {
            BlockPos target = partPos(masterPos, state, offset);
            BlockState partState = ModBlocks.TURBINE_PART.get().defaultBlockState()
                    .setValue(TurbinePartBlock.FACING, facing)
                    .setValue(TurbinePartBlock.WIND, wind)
                    .setValue(TurbinePartBlock.LOCAL_X, TurbinePartBlock.encodeLocalX(offset.x()))
                    .setValue(TurbinePartBlock.LOCAL_Y, offset.y());
            level.setBlock(target, partState, Block.UPDATE_ALL);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        return useAt(level, pos, player);
    }

    public static InteractionResult useAt(Level level, BlockPos masterPos, Player player) {
        return InteractionResult.PASS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        for (PartOffset offset : parts(state)) {
            BlockPos partPos = partPos(pos, state, offset);
            if (level.getBlockState(partPos).is(ModBlocks.TURBINE_PART.get())) {
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.is(ModBlocks.WATER_TURBINE.get())) {
            return TurbinePartBlock.waterShape(state.getValue(FACING), 0);
        }
        return WIND_BASE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public static List<PartOffset> parts(BlockState state) {
        return state.is(ModBlocks.WATER_TURBINE.get()) ? WATER_PARTS : WIND_PARTS;
    }

    public static BlockPos partPos(BlockPos masterPos, BlockState state, PartOffset offset) {
        Direction right = state.getValue(FACING).getClockWise();
        return masterPos.relative(right, offset.x()).above(offset.y());
    }

    public static List<BlockPos> structurePositions(BlockPos masterPos, BlockState state) {
        List<BlockPos> positions = new java.util.ArrayList<>(parts(state).size() + 1);
        positions.add(masterPos.immutable());
        for (PartOffset offset : parts(state)) {
            positions.add(partPos(masterPos, state, offset).immutable());
        }
        return List.copyOf(positions);
    }

    public record PartOffset(int x, int y) {
    }
}
