package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TurbinePartBlock extends Block {
    public static final MapCodec<TurbinePartBlock> CODEC = simpleCodec(TurbinePartBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WIND = BooleanProperty.create("wind");
    public static final IntegerProperty LOCAL_X = IntegerProperty.create("local_x", 0, 4);
    public static final IntegerProperty LOCAL_Y = IntegerProperty.create("local_y", 0, 3);

    public TurbinePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WIND, true)
                .setValue(LOCAL_X, encodeLocalX(0))
                .setValue(LOCAL_Y, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        int localX = decodeLocalX(state.getValue(LOCAL_X));
        int localY = state.getValue(LOCAL_Y);
        return state.getValue(WIND)
                ? windShape(facing, localX, localY)
                : waterShape(facing, localX);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        BlockPos masterPos = masterPos(pos, state);
        if (isExpectedMaster(level, masterPos, state)) {
            return TurbineBlock.useAt(level, masterPos, player);
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos masterPos = masterPos(pos, state);
            if (isExpectedMaster(level, masterPos, state)) {
                level.destroyBlock(masterPos, !player.getAbilities().instabuild, player, Block.UPDATE_LIMIT);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockPos masterPos = masterPos(pos, state);
        if (isExpectedMaster(level, masterPos, state)) {
            level.destroyBlock(masterPos, true);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WIND, LOCAL_X, LOCAL_Y);
    }

    public static BlockPos masterPos(BlockPos partPos, BlockState state) {
        Direction right = state.getValue(FACING).getClockWise();
        return partPos.relative(right, -decodeLocalX(state.getValue(LOCAL_X))).below(state.getValue(LOCAL_Y));
    }

    public static boolean isExpectedMaster(Level level, BlockPos masterPos, BlockState partState) {
        BlockState masterState = level.getBlockState(masterPos);
        return masterState.is(partState.getValue(WIND)
                ? ModBlocks.WIND_TURBINE.get()
                : ModBlocks.WATER_TURBINE.get())
                && masterState.getValue(TurbineBlock.FACING) == partState.getValue(FACING);
    }

    static VoxelShape waterShape(Direction facing, int localX) {
        double minWidth = localX == -1 ? 8.0D : 0.0D;
        double maxWidth = localX == 1 ? 8.0D : 16.0D;
        if (facing.getAxis() == Direction.Axis.Z) {
            return Block.box(minWidth, 0.0D, 1.0D, maxWidth, 16.0D, 15.0D);
        }
        return Block.box(1.0D, 0.0D, minWidth, 15.0D, 16.0D, maxWidth);
    }

    public static int encodeLocalX(int localX) {
        return localX + 2;
    }

    public static int decodeLocalX(int encoded) {
        return encoded - 2;
    }

    private static VoxelShape windShape(Direction facing, int localX, int localY) {
        if (localY == 3) {
            return facing.getAxis() == Direction.Axis.Z
                    ? Block.box(6.0D, 0.0D, 4.0D, 10.0D, 7.0D, 12.0D)
                    : Block.box(4.0D, 0.0D, 6.0D, 12.0D, 7.0D, 10.0D);
        }

        double minY = localY == 1 ? 14.0D : 0.0D;
        double maxY = localY == 1 ? 16.0D : 3.0D;
        VoxelShape rotor = facing.getAxis() == Direction.Axis.Z
                ? Block.box(0.0D, minY, 4.0D, 16.0D, maxY, 12.0D)
                : Block.box(4.0D, minY, 0.0D, 12.0D, maxY, 16.0D);
        if (localX != 0) {
            return rotor;
        }
        VoxelShape mast = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);
        return Shapes.or(rotor, mast);
    }
}
