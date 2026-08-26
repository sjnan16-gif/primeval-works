package com.primevalworks.client.render.item;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;
import com.primevalworks.client.model.item.LaserTurretItemGeoModel;
import com.primevalworks.world.item.LaserTurretBlockItem;
import net.minecraft.world.item.ItemDisplayContext;

public final class LaserTurretItemRenderer extends GeoItemRenderer<LaserTurretBlockItem> {
    public LaserTurretItemRenderer() {
        super(new LaserTurretItemGeoModel());
        useAlternateGuiLighting();
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<GeoRenderState> renderPassInfo,
                                    float widthScale, float heightScale) {
        ItemDisplayContext context = renderPassInfo.getOrDefaultGeckolibData(
                DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
        float scale = switch (context) {
            case GUI -> 0.47F;
            case GROUND -> 0.34F;
            case FIXED, ON_SHELF -> 0.48F;
            default -> 0.43F;
        };
        super.scaleModelForRender(renderPassInfo, widthScale * scale, heightScale * scale);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        super.adjustRenderPose(renderPassInfo);
        ItemDisplayContext context = renderPassInfo.getOrDefaultGeckolibData(
                DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
        if (context == ItemDisplayContext.GUI) {
            renderPassInfo.poseStack().translate(0.0F, -0.09F, 0.0F);
            renderPassInfo.poseStack().mulPose(Axis.XP.rotationDegrees(18.0F));
            renderPassInfo.poseStack().mulPose(Axis.YP.rotationDegrees(-36.0F));
        }
    }
}
