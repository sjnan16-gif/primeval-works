package com.primevalworks.client.render.block;

import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.client.model.block.DinosaurEggGeoModel;
import com.primevalworks.world.block.DinosaurEggBlock;
import com.primevalworks.world.block.entity.DinosaurEggBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class DinosaurEggRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<DinosaurEggBlockEntity, R> {
    public DinosaurEggRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new DinosaurEggGeoModel());
    }

    @Override
    public void addRenderData(DinosaurEggBlockEntity egg, Void relatedObject,
                              R renderState, float partialTick) {
        if (egg.getBlockState().getBlock() instanceof DinosaurEggBlock block) {
            renderState.addGeckolibData(DinosaurEggGeoModel.SIZE, block.size());
        }
    }

    @Override
    public RenderType getRenderType(R renderState, Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }
}
