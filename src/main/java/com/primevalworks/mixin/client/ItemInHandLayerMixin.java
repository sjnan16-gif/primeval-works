package com.primevalworks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.primevalworks.client.effect.BayonetAnimationCurve;
import com.primevalworks.registry.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V")
    )
    private void primevalworks$animateBayonetItem(
            ArmedEntityRenderState state,
            ItemStackRenderState item,
            ItemStack itemStack,
            HumanoidArm arm,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo callback
    ) {
        if (!itemStack.is(ModItems.ANCIENT_REFORGED_BAYONET.get())
                || state.attackArm != arm
                || state.attackTime <= 0.0F) {
            return;
        }
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        BayonetAnimationCurve.Sample animation = BayonetAnimationCurve.sample(state.attackTime);
        float wobble = (float)Math.sin(animation.retract() * Math.PI * 3.0D)
                * (1.0F - animation.retract());
        poseStack.translate(0.0F, -animation.extension() * 0.03F, -animation.extension() * 0.18F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * animation.orientationDegrees()));
        poseStack.mulPose(Axis.XP.rotationDegrees(wobble * 7.0F));
    }
}
