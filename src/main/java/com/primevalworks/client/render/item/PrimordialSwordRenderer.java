package com.primevalworks.client.render.item;

import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.primevalworks.client.model.item.PrimordialSwordGeoModel;
import com.primevalworks.world.item.PrimordialSwordItem;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class PrimordialSwordRenderer extends GeoItemRenderer<PrimordialSwordItem> {
    public PrimordialSwordRenderer() {
        super(new PrimordialSwordGeoModel());
    }

    @Override
    public RenderType getRenderType(GeoRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }
}
