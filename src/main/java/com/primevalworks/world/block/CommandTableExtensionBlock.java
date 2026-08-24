package com.primevalworks.world.block;

import com.mojang.serialization.MapCodec;
import com.primevalworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public final class CommandTableExtensionBlock extends Block {
    public static final MapCodec<CommandTableExtensionBlock> CODEC = simpleCodec(CommandTableExtensionBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.empty();

    public CommandTableExtensionBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public BlockPos masterPos(BlockPos extensionPos, BlockState state) {
        return extensionPos.relative(state.getValue(FACING).getCounterClockWise());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (direction == state.getValue(FACING).getCounterClockWise()
                && (!neighborState.is(ModBlocks.COMMAND_TABLE.get())
                || neighborState.getValue(CommandTableBlock.FACING) != state.getValue(FACING))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos masterPos = masterPos(pos, state);
            if (level.getBlockState(masterPos).is(ModBlocks.COMMAND_TABLE.get())) {
                if (player instanceof ServerPlayer serverPlayer) {
                    CommandTableBlock.releaseClaim(serverPlayer, masterPos);
                }
                level.destroyBlock(masterPos, true, player, Block.UPDATE_LIMIT);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
