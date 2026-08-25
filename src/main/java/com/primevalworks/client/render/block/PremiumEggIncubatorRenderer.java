package com.primevalworks.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.primevalworks.world.block.entity.PremiumEggIncubatorBlockEntity;
import com.primevalworks.world.egg.DinosaurEggSize;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class PremiumEggIncubatorRenderer
        implements BlockEntityRenderer<PremiumEggIncubatorBlockEntity, IncubatorRenderState> {
    private final ItemModelResolver itemModelResolver;

    public PremiumEggIncubatorRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public IncubatorRenderState createRenderState() {
        return new IncubatorRenderState();
    }

    @Override
    public void extractRenderState(
            PremiumEggIncubatorBlockEntity incubator,
            IncubatorRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(incubator, state, partialTick, cameraPosition, breakProgress);
        state.active = incubator.hasEgg();
        state.eggSize = DinosaurEggSize.fromItem(incubator.getEgg()).orElse(DinosaurEggSize.SMALL);
        state.progress = incubator.getProgressFraction();
        state.animationTime = incubator.getLevel() == null ? 0.0F : incubator.getLevel().getGameTime() + partialTick;
        itemModelResolver.updateForTopItem(
                state.egg,
                incubator.getEgg(),
                ItemDisplayContext.FIXED,
                incubator.getLevel(),
                null,
                (int)incubator.getBlockPos().asLong()
        );
        int seconds = Mth.ceil(incubator.getRemainingTicks() / 20.0F);
        state.timer = Component.translatable(
                "render.primevalworks.incubator.timer",
                seconds / 60,
                seconds % 60,
                Mth.floor(state.progress * 100.0F)
        ).getVisualOrderText();
    }

    @Override
    public void submit(
            IncubatorRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (!state.active || state.egg.isEmpty()) {
            return;
        }

        float settle = Mth.clamp(state.animationTime / 10.0F, 0.0F, 1.0F);
        float bob = Mth.sin(state.animationTime * 0.045F) * 0.004F;
        float modelHeight = state.eggSize == DinosaurEggSize.SMALL ? 0.6875F : 1.0F;
        poseStack.pushPose();
        poseStack.translate(0.5F, IncubatorEggFit.centerYForModelHeight(modelHeight) + bob, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(state.animationTime * 0.035F) * 0.8F));
        float eggScale = IncubatorEggFit.scaleForModelHeight(modelHeight) * (0.90F + 0.10F * settle);
        poseStack.scale(eggScale, eggScale, eggScale);
        state.egg.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.34F + bob * 0.45F, 0.5F);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.018F, -0.018F, 0.018F);
        int width = net.minecraft.client.Minecraft.getInstance().font.width(state.timer);
        int textColor = Mth.lerpInt(state.progress, 0xFFE8D8B5, 0xFFFFD96A);
        submitNodeCollector.order(4).submitText(
                poseStack,
                -width / 2.0F,
                0.0F,
                state.timer,
                true,
                Font.DisplayMode.SEE_THROUGH,
                15728880,
                textColor,
                0x820F0C09,
                0
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PremiumEggIncubatorBlockEntity incubator) {
        BlockPos pos = incubator.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 2.0D, pos.getZ() + 1.0D);
    }
}
