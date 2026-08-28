package com.primevalworks.client.screen;

import com.primevalworks.client.model.entity.DinosaurPreviewBounds;
import com.primevalworks.client.model.entity.DinosaurVisualProfile;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class DinosaurPreviewUi {
    private DinosaurPreviewUi() {}

    public static void draw(GuiGraphicsExtractor graphics, FieldDodoEntity dinosaur,
                            float x, float y, float width, float height,
                            float viewYaw, float viewPitch) {
        int x0 = Mth.ceil(x);
        int y0 = Mth.ceil(y);
        int x1 = Mth.floor(x + width);
        int y1 = Mth.floor(y + height);
        if (x1 <= x0 || y1 <= y0) return;
        DinosaurVisualProfile visual = DinosaurVisualProfile.forType(dinosaur.getType());
        float scale = previewScale(width, height, visual, viewYaw, viewPitch);
        float renderCenterX = (x0 + x1) * 0.5F;
        float renderCenterY = (y0 + y1) * 0.5F;
        Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf topDownRotation = new Quaternionf().rotateX(viewPitch * Mth.DEG_TO_RAD);
        rotation.mul(topDownRotation);
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRenderer<? super FieldDodoEntity, ?> renderer = dispatcher.getRenderer(dinosaur);
        EntityRenderState renderState = renderer.createRenderState(dinosaur, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = 180.0F - viewYaw;
            livingState.yRot = 0.0F;
            livingState.xRot = 0.0F;
            livingState.boundingBoxWidth /= livingState.scale;
            livingState.boundingBoxHeight /= livingState.scale;
            livingState.scale = 1.0F;
        }
        float inverseScale = 1.0F / Math.max(0.001F, scale);
        Vector3f translation = new Vector3f(
                (x + width * 0.5F - renderCenterX) * inverseScale,
                renderState.boundingBoxHeight * 0.5F + visual.modelGroundOffset()
                        + (y + height * 0.5F - renderCenterY) * inverseScale,
                0.0F);
        graphics.entity(renderState, scale, translation, rotation, topDownRotation, x0, y0, x1, y1);
    }

    private static float previewScale(float width, float height, DinosaurVisualProfile visual,
                                      float viewYaw, float viewPitch) {
        DinosaurPreviewBounds bounds = DinosaurPreviewBounds.forVisual(visual);
        float yaw = viewYaw * Mth.DEG_TO_RAD;
        float pitch = viewPitch * Mth.DEG_TO_RAD;
        float footprint = Math.abs(bounds.width() * Mth.cos(yaw)) + Math.abs(bounds.depth() * Mth.sin(yaw));
        float cameraDepth = Math.abs(bounds.width() * Mth.sin(yaw)) + Math.abs(bounds.depth() * Mth.cos(yaw));
        float projectedHeight = bounds.height() * Math.abs(Mth.cos(pitch))
                + cameraDepth * Math.abs(Mth.sin(pitch));
        return Mth.clamp(Math.min(width / Math.max(0.35F, footprint),
                height / Math.max(0.35F, projectedHeight)) * 0.97F, 1.5F, 44.0F);
    }
}
