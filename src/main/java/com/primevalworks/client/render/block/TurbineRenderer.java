package com.primevalworks.client.render.block;

import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.client.model.block.TurbineGeoModel;
import com.primevalworks.world.block.entity.TurbineBlockEntity;
import com.primevalworks.world.block.TurbineBlock;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
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
                TurbineBlock.isWindTurbine(turbine.getBlockState()));
        renderState.addGeckolibData(TurbineGeoModel.UPGRADED,
                TurbineBlock.isUpgradedWindTurbine(turbine.getBlockState()));
    }

    @Override
    public RenderType getRenderType(R renderState, Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }

    @Override
    public AABB getRenderBoundingBox(TurbineBlockEntity turbine) {
        BlockPos pos = turbine.getBlockPos();
        Direction facing = turbine.getBlockState().getValue(com.primevalworks.world.block.TurbineBlock.FACING);
        boolean widthRunsEastWest = facing.getAxis() == Direction.Axis.Z;
        if (TurbineBlock.isWindTurbine(turbine.getBlockState())) {
            return widthRunsEastWest
                    ? new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ(),
                            pos.getX() + 2.0D, pos.getY() + 4.0D, pos.getZ() + 1.0D)
                    : new AABB(pos.getX(), pos.getY(), pos.getZ() - 1.0D,
                            pos.getX() + 1.0D, pos.getY() + 4.0D, pos.getZ() + 2.0D);
        }
        return widthRunsEastWest
                ? new AABB(pos.getX() - 2.0D, pos.getY(), pos.getZ(),
                        pos.getX() + 3.0D, pos.getY() + 3.0D, pos.getZ() + 1.0D)
                : new AABB(pos.getX(), pos.getY(), pos.getZ() - 2.0D,
                        pos.getX() + 1.0D, pos.getY() + 3.0D, pos.getZ() + 3.0D);
    }
}
