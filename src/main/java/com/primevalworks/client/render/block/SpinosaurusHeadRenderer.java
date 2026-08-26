package com.primevalworks.client.render.block;

import com.geckolib.renderer.GeoBlockRenderer;
import com.primevalworks.client.model.block.SpinosaurusHeadGeoModel;
import com.primevalworks.world.block.entity.SpinosaurusHeadBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class SpinosaurusHeadRenderer
        extends GeoBlockRenderer<SpinosaurusHeadBlockEntity, BlockEntityRenderState> {
    public SpinosaurusHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new SpinosaurusHeadGeoModel());
        withScale(0.68F);
    }

    @Override
    public AABB getRenderBoundingBox(SpinosaurusHeadBlockEntity head) {
        BlockPos pos = head.getBlockPos();
        return new AABB(pos).inflate(2.0D);
    }
}
