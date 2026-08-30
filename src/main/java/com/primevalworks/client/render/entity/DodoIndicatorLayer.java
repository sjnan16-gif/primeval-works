package com.primevalworks.client.render.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class DodoIndicatorLayer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoRenderLayer<FieldDodoEntity, Void, R> {
    private static final DataTicket<Integer> ICON = DataTicket.create("primevalworks_dodo_indicator", Integer.class);
    private static final DataTicket<Integer> AGE = DataTicket.create("primevalworks_dodo_indicator_age", Integer.class);
    private static final DataTicket<Integer> ICON_AGE = DataTicket.create("primevalworks_dodo_icon_age", Integer.class);
    private static final DataTicket<Integer> ICON_TICKS = DataTicket.create("primevalworks_dodo_icon_ticks", Integer.class);
    private static final DataTicket<Integer> SLEEP_AGE = DataTicket.create("primevalworks_dodo_sleep_age", Integer.class);
    private static final DataTicket<Float> PARTIAL_TICK = DataTicket.create("primevalworks_dodo_indicator_partial", Float.class);
    private static final DataTicket<Boolean> SLEEPING = DataTicket.create("primevalworks_dodo_sleeping", Boolean.class);
    private static final DataTicket<ItemStackRenderState> CARGO = DataTicket.create("primevalworks_dodo_cargo", ItemStackRenderState.class);
    private static final DataTicket<Integer> CARGO_COUNT = DataTicket.create("primevalworks_dodo_cargo_count", Integer.class);
    private static final DataTicket<Float> CARGO_SCALE = DataTicket.create("primevalworks_dodo_cargo_scale", Float.class);
    private static final Identifier HAPPY = Identifier.fromNamespaceAndPath("primevalworks", "textures/entity/indicator/happy.png");
    private static final Identifier NEUTRAL = Identifier.fromNamespaceAndPath("primevalworks", "textures/entity/indicator/neutral.png");
    private static final Identifier SAD = Identifier.fromNamespaceAndPath("primevalworks", "textures/entity/indicator/sad.png");
    private static final Identifier FOOD = Identifier.withDefaultNamespace("textures/gui/sprites/hud/food_full.png");
    private static final Identifier UI_CROP_TEXTURE = Identifier.fromNamespaceAndPath(
            "primevalworks",
            "textures/gui/ui_crop.png"
    );
    private final float indicatorHeight;
    private final float scaleCompensation;
    private final float statusIconScale;
    private final ItemModelResolver itemModelResolver;

    public DodoIndicatorLayer(
            EntityRendererProvider.Context context,
            GeoRenderer<FieldDodoEntity, Void, R> renderer,
            float indicatorHeight,
            float scaleCompensation,
            float statusIconScale
    ) {
        super(renderer);
        this.indicatorHeight = indicatorHeight;
        this.scaleCompensation = scaleCompensation;
        this.statusIconScale = statusIconScale;
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public void addRenderData(FieldDodoEntity dodo, Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(ICON, dodo.getIndicatorIcon());
        renderState.addGeckolibData(AGE, dodo.tickCount);
        renderState.addGeckolibData(ICON_AGE, dodo.getIndicatorAge());
        renderState.addGeckolibData(ICON_TICKS, dodo.getIndicatorTicks());
        renderState.addGeckolibData(SLEEP_AGE, dodo.getSleepVisualTicks());
        renderState.addGeckolibData(PARTIAL_TICK, partialTick);
        renderState.addGeckolibData(SLEEPING, dodo.isDinosaurSleeping());
        ItemStack cargo = dodo.getCarriedStack();
        if (!cargo.isEmpty()) {
            renderState.addGeckolibData(CARGO, RenderUtil.createRenderStateForItem(
                    cargo,
                    itemModelResolver,
                    ItemDisplayContext.GUI,
                    dodo
            ));
            renderState.addGeckolibData(CARGO_COUNT, cargo.getCount());
            renderState.addGeckolibData(CARGO_SCALE, dodo.getRenderedCargoScale());
        }
    }

    @Override
    public void submitRenderTask(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        if (renderPassInfo.getOrDefaultGeckolibData(FieldDodoRenderer.DEFEAT_PROGRESS, 0.0F) > 0.0F) return;
        submitCargo(renderPassInfo, renderTasks);
        boolean sleeping = renderPassInfo.getOrDefaultGeckolibData(SLEEPING, false);
        if (sleeping) {
            submitSleepingGlyphs(renderPassInfo, renderTasks);
            return;
        }

        int icon = renderPassInfo.getOrDefaultGeckolibData(ICON, 0);
        boolean foodTakesPriority = icon == 4 || icon == 5;
        Identifier texture = foodTakesPriority ? FOOD : moodTexture(icon);
        if (texture == null) return;

        PoseStack pose = renderPassInfo.poseStack();
        pose.pushPose();
        int age = renderPassInfo.getOrDefaultGeckolibData(AGE, 0);
        int iconAge = renderPassInfo.getOrDefaultGeckolibData(ICON_AGE, 0);
        int iconTicks = renderPassInfo.getOrDefaultGeckolibData(ICON_TICKS, 0);
        float partialTick = renderPassInfo.getOrDefaultGeckolibData(PARTIAL_TICK, 0.0F);
        float motionAge = iconAge + partialTick;
        float bob = Mth.sin((age + partialTick) * 0.095F) * 0.028F;
        float appear = spring(Math.min(1.0F, motionAge / 10.0F));
        float disappear = smoothStep(Math.min(1.0F, iconTicks / 8.0F));
        float motionScale = Math.max(0.0F, appear) * disappear;
        float wobble = (float) Math.sin(motionAge * 0.86F) * 11.0F * (1.0F - Math.min(1.0F, motionAge / 14.0F));
        float distanceScale = distanceScale(renderPassInfo.renderState().distanceToCameraSq);
        pose.translate(0.0F, indicatorHeight + bob * scaleCompensation, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(renderPassInfo.renderState().bodyRot - 180.0F));
        pose.mulPose(renderPassInfo.cameraState().orientation);
        pose.mulPose(Axis.ZP.rotationDegrees(wobble));
        float scaleX = (icon == 5 ? 0.78F : 0.72F) * statusIconScale;
        pose.scale(scaleX * motionScale * distanceScale * scaleCompensation,
                scaleX * motionScale * distanceScale * scaleCompensation,
                scaleX * motionScale * distanceScale * scaleCompensation);
        renderTasks.submitCustomGeometry(pose, RenderTypes.entityTranslucent(texture), (matrix, vertices) -> {
            int light = 0x00F000F0;
            int overlay = OverlayTexture.NO_OVERLAY;
            addQuad(vertices, matrix, -0.5F, 0.0F, 0.5F, 1.0F, 0.0F, 0xFFFFFFFF, overlay, light);
        });
        pose.popPose();
    }

    private void submitCargo(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        ItemStackRenderState cargo = renderPassInfo.renderState().getGeckolibData(CARGO);
        float appear = renderPassInfo.getOrDefaultGeckolibData(CARGO_SCALE, 0.0F);
        if (cargo == null || cargo.isEmpty() || appear <= 0.01F) {
            return;
        }

        int age = renderPassInfo.getOrDefaultGeckolibData(AGE, 0);
        float partialTick = renderPassInfo.getOrDefaultGeckolibData(PARTIAL_TICK, 0.0F);
        float motionAge = age + partialTick;
        float bob = Mth.sin(motionAge * 0.13F) * 0.055F;
        float distanceScale = distanceScale(renderPassInfo.renderState().distanceToCameraSq);
        float frameScale = 0.86F * appear * distanceScale * scaleCompensation;
        PoseStack pose = renderPassInfo.poseStack();
        pose.pushPose();
        pose.translate(0.0F, indicatorHeight + (0.48F + bob) * scaleCompensation, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(renderPassInfo.renderState().bodyRot - 180.0F));
        pose.mulPose(renderPassInfo.cameraState().orientation);
        pose.scale(frameScale, frameScale, frameScale);
        renderTasks.submitCustomGeometry(pose, RenderTypes.entityTranslucent(UI_CROP_TEXTURE), (matrix, vertices) ->
                addCargoSlotQuad(vertices, matrix, 0xFFFFFFFF, OverlayTexture.NO_OVERLAY, 0x00F000F0));
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0F, indicatorHeight + (0.48F + bob) * scaleCompensation, -0.012F);
        pose.mulPose(Axis.YP.rotationDegrees(renderPassInfo.renderState().bodyRot - 180.0F));
        pose.mulPose(renderPassInfo.cameraState().orientation);
        float itemScale = 0.44F * appear * distanceScale * scaleCompensation;
        pose.scale(itemScale, itemScale, itemScale);
        cargo.submit(pose, renderTasks, 0x00F000F0, OverlayTexture.NO_OVERLAY, renderPassInfo.renderState().outlineColor);
        pose.popPose();

        int count = renderPassInfo.getOrDefaultGeckolibData(CARGO_COUNT, 0);
        if (count <= 1) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        var text = Component.literal(Integer.toString(count)).getVisualOrderText();
        pose.pushPose();
        pose.translate(0.24F * scaleCompensation, indicatorHeight + (0.19F + bob) * scaleCompensation, -0.055F);
        pose.mulPose(Axis.YP.rotationDegrees(renderPassInfo.renderState().bodyRot - 180.0F));
        pose.mulPose(renderPassInfo.cameraState().orientation);
        float textScale = 0.030F * appear * distanceScale * scaleCompensation;
        pose.scale(-textScale, -textScale, textScale);
        renderTasks.submitText(
                pose,
                -font.width(text) / 2.0F,
                0.0F,
                text,
                true,
                Font.DisplayMode.SEE_THROUGH,
                0x00F000F0,
                0xFFFFF1CF,
                0xC020140B,
                renderPassInfo.renderState().outlineColor
        );
        pose.popPose();
    }

    private void submitSleepingGlyphs(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        Font font = Minecraft.getInstance().font;
        var glyph = Component.literal("Z").getVisualOrderText();
        int age = renderPassInfo.getOrDefaultGeckolibData(SLEEP_AGE, 0);
        float partialTick = renderPassInfo.getOrDefaultGeckolibData(PARTIAL_TICK, 0.0F);
        float sleepAge = age + partialTick;
        float distanceScale = distanceScale(renderPassInfo.renderState().distanceToCameraSq);
        for (int index = 0; index < 3; index++) {
            float phase = positiveModulo(sleepAge + index * 15.0F, 72.0F);
            float progress = phase / 72.0F;
            float appear = smoothStep(Math.min(1.0F, phase / 8.0F));
            float fade = 1.0F - smoothStep((phase - 42.0F) / 30.0F);
            float opacity = Mth.clamp(appear * fade, 0.0F, 1.0F);
            if (opacity <= 0.015F) {
                continue;
            }

            float drift = (float) Math.sin(phase * 0.09F + index * 1.7F) * 0.045F;
            float side = -0.20F + index * 0.20F + drift;
            float rise = index * 0.08F + progress * 0.62F;
            float glyphScale = 0.040F
                    * statusIconScale
                    * distanceScale
                    * scaleCompensation
                    * (0.92F + index * 0.14F)
                    * (0.78F + appear * 0.22F);
            int alpha = Mth.clamp(Math.round(opacity * 255.0F), 0, 255);
            int color = (alpha << 24) | 0x00FFFFFF;

            PoseStack pose = renderPassInfo.poseStack();
            pose.pushPose();
            pose.translate(side * scaleCompensation, indicatorHeight + rise * scaleCompensation, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(renderPassInfo.renderState().bodyRot - 180.0F));
            pose.mulPose(renderPassInfo.cameraState().orientation);
            pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(phase * 0.08F + index) * 3.0F));
            pose.scale(-glyphScale, -glyphScale, glyphScale);
            renderTasks.submitText(
                    pose,
                    -font.width(glyph) * 0.5F,
                    -font.lineHeight * 0.5F,
                    glyph,
                    true,
                    Font.DisplayMode.SEE_THROUGH,
                    0x00F000F0,
                    color,
                    (alpha << 24) | 0x00130E0A,
                    renderPassInfo.renderState().outlineColor
            );
            pose.popPose();
        }
    }

    private static Identifier moodTexture(int icon) {
        return switch (icon) {
            case 1 -> HAPPY;
            case 2 -> NEUTRAL;
            case 3 -> SAD;
            case 4, 5 -> FOOD;
            default -> null;
        };
    }

    private static float distanceScale(double distanceSquared) {
        double distance = Math.sqrt(Math.max(0.0D, distanceSquared));
        return (float) Math.min(1.55D, Math.max(1.0D, 1.0D + Math.max(0.0D, distance - 8.0D) * 0.014D));
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float spring(float value) {
        if (value >= 1.0F) return 1.0F;
        double damping = 6.2D;
        double frequency = 10.5D;
        double wave = Math.cos(frequency * value) + damping / frequency * Math.sin(frequency * value);
        return 1.0F - (float) (Math.exp(-damping * value) * wave);
    }

    private static float positiveModulo(float value, float modulus) {
        float result = value % modulus;
        return result < 0.0F ? result + modulus : result;
    }

    private static void addQuad(
            VertexConsumer vertices,
            PoseStack.Pose matrix,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3,
            float x4,
            float y4,
            float z,
            int color,
            int overlay,
            int light
    ) {
        vertices.addVertex(matrix, x1, y1, z).setColor(color).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, x2, y2, z).setColor(color).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, x3, y3, z).setColor(color).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, x4, y4, z).setColor(color).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
    }

    private static void addQuad(
            VertexConsumer vertices,
            PoseStack.Pose matrix,
            float left,
            float bottom,
            float right,
            float top,
            float z,
            int color,
            int overlay,
            int light
    ) {
        vertices.addVertex(matrix, left, bottom, z).setColor(color).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, right, bottom, z).setColor(color).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, right, top, z).setColor(color).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, left, top, z).setColor(color).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
    }

    private static void addCargoSlotQuad(
            VertexConsumer vertices,
            PoseStack.Pose matrix,
            int color,
            int overlay,
            int light
    ) {
        float left = -0.5F;
        float right = 0.5F;
        float bottom = -19.0F / 36.0F;
        float top = 19.0F / 36.0F;
        float u1 = 117.0F / 427.0F;
        float v1 = 75.0F / 240.0F;
        float u2 = 135.0F / 427.0F;
        float v2 = 94.0F / 240.0F;
        vertices.addVertex(matrix, left, bottom, 0.0F).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, right, bottom, 0.0F).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, right, top, 0.0F).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(matrix, left, top, 0.0F).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
    }
}
