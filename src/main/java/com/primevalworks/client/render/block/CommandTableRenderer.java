package com.primevalworks.client.render.block;

import com.geckolib.renderer.GeoBlockRenderer;
import com.primevalworks.client.model.block.CommandTableGeoModel;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class CommandTableRenderer
        extends GeoBlockRenderer<CommandTableBlockEntity, BlockEntityRenderState> {
    public CommandTableRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new CommandTableGeoModel());
    }

    @Override
    public AABB getRenderBoundingBox(CommandTableBlockEntity table) {
        BlockPos pos = table.getBlockPos();
        return new AABB(pos.getX() - 0.25D, pos.getY(), pos.getZ() - 0.25D,
                pos.getX() + 1.25D, pos.getY() + 1.5D, pos.getZ() + 1.25D);
    }
}
