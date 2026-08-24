package com.primevalworks.client.render.entity;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.primevalworks.client.model.entity.DinosaurVisualProfile;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class DinosaurMouthItemLayer<R extends LivingEntityRenderState & GeoRenderState>
        extends BlockAndItemGeoLayer<FieldDodoEntity, Void, R> {
    private final DinosaurVisualProfile profile;

    public DinosaurMouthItemLayer(
            EntityRendererProvider.Context context,
            GeoRenderer<FieldDodoEntity, Void, R> renderer,
            DinosaurVisualProfile profile
    ) {
        super(context, renderer);
        this.profile = profile;
    }

    @Override
    protected List<RenderData> getRelevantBones(
            FieldDodoEntity dinosaur,
            Void relatedObject,
            R renderState,
            float partialTick
    ) {
        ItemStack carried = dinosaur.getCarriedStack();
        if (carried.isEmpty()) {
            return List.of();
        }
        ItemStackRenderState itemState = RenderUtil.createRenderStateForItem(
                carried,
                itemModelResolver,
                ItemDisplayContext.FIXED,
                dinosaur
        );
        return List.of(RenderData.item(profile.carryBone(), ItemDisplayContext.FIXED, itemState));
    }

    @Override
    public void addRenderData(FieldDodoEntity dinosaur, Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(CONTENTS, getRelevantBones(dinosaur, relatedObject, renderState, partialTick));
    }

    @Override
    protected void submitItemStackRender(
            PoseStack poseStack,
            GeoBone bone,
            ItemStackRenderState stackState,
            ItemDisplayContext displayContext,
            R renderState,
            SubmitNodeCollector renderTasks,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        float scale = profile.carriedItemScale();
        poseStack.scale(scale, scale, scale);
        super.submitItemStackRender(poseStack, bone, stackState, displayContext, renderState, renderTasks, packedLight);
        poseStack.popPose();
    }
}
