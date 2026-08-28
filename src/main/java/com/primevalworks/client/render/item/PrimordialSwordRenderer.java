package com.primevalworks.client.render.item;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.primevalworks.client.model.item.PrimordialSwordGeoModel;
import com.primevalworks.world.item.PrimordialSwordItem;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

public final class PrimordialSwordRenderer extends GeoItemRenderer<PrimordialSwordItem> {
    public PrimordialSwordRenderer() {
        super(new PrimordialSwordGeoModel());
    }

    @Override
    public RenderType getRenderType(GeoRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<GeoRenderState> renderPassInfo,
                                    float widthScale, float heightScale) {
        ItemDisplayContext context = renderPassInfo.getOrDefaultGeckolibData(
                DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
        float scale = switch (context) {
            case GROUND -> 0.54F;
            case FIXED, ON_SHELF -> 0.66F;
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> 0.76F;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> 0.70F;
            default -> 0.72F;
        };
        super.scaleModelForRender(renderPassInfo, widthScale * scale, heightScale * scale);
    }
}
