package com.primevalworks.client.render.block;

import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.primevalworks.client.model.block.TurbineGeoModel;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.entity.TurbineBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class TurbineRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<TurbineBlockEntity, R> {
    public TurbineRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new TurbineGeoModel());
    }

    @Override
    public void addRenderData(TurbineBlockEntity turbine, Void relatedObject,
                              R renderState, float partialTick) {
        renderState.addGeckolibData(TurbineGeoModel.WIND,
                turbine.getBlockState().is(ModBlocks.WIND_TURBINE.get()));
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<R> renderPassInfo, float widthScale, float heightScale) {
        boolean wind = renderPassInfo.getOrDefaultGeckolibData(TurbineGeoModel.WIND, false);
        super.scaleModelForRender(renderPassInfo, widthScale, wind ? heightScale : heightScale * 2.0F);
    }

    @Override
    public AABB getRenderBoundingBox(TurbineBlockEntity turbine) {
        BlockPos pos = turbine.getBlockPos();
        Direction facing = turbine.getBlockState().getValue(com.primevalworks.world.block.TurbineBlock.FACING);
        boolean widthRunsEastWest = facing.getAxis() == Direction.Axis.Z;
        if (turbine.getBlockState().is(ModBlocks.WIND_TURBINE.get())) {
            return widthRunsEastWest
                    ? new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ(),
                            pos.getX() + 2.0D, pos.getY() + 4.0D, pos.getZ() + 1.0D)
                    : new AABB(pos.getX(), pos.getY(), pos.getZ() - 1.0D,
                            pos.getX() + 1.0D, pos.getY() + 4.0D, pos.getZ() + 2.0D);
        }
        return widthRunsEastWest
                ? new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ(),
                        pos.getX() + 2.0D, pos.getY() + 2.0D, pos.getZ() + 1.0D)
                : new AABB(pos.getX(), pos.getY(), pos.getZ() - 1.0D,
                        pos.getX() + 1.0D, pos.getY() + 2.0D, pos.getZ() + 2.0D);
    }
}
