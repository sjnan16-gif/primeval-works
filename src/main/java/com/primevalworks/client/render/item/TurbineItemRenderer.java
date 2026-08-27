package com.primevalworks.client.render.item;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.primevalworks.client.model.item.TurbineItemGeoModel;
import com.primevalworks.world.item.TurbineBlockItem;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

public final class TurbineItemRenderer extends GeoItemRenderer<TurbineBlockItem> {
    private final TurbineBlockItem.Variant variant;

    public TurbineItemRenderer(TurbineBlockItem.Variant variant) {
        super(new TurbineItemGeoModel(variant));
        this.variant = variant;
        useAlternateGuiLighting();
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
            case GUI -> 0.48F;
            case GROUND -> 0.38F;
            case FIXED -> 0.40F;
            case ON_SHELF -> 0.30F;
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> 0.33F;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> 0.45F;
            default -> 0.42F;
        };
        super.scaleModelForRender(renderPassInfo, widthScale * scale, heightScale * scale);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        super.adjustRenderPose(renderPassInfo);

        ItemDisplayContext context = renderPassInfo.getOrDefaultGeckolibData(
                DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
        if (context == ItemDisplayContext.GUI) {
            renderPassInfo.poseStack().translate(
                    variant.guiOffsetX(), variant.guiOffsetY(), variant.guiOffsetZ());
        }
    }
}
