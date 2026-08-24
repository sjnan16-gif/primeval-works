package com.primevalworks.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class BayonetClientExtension implements IClientItemExtensions {
    @Override
    public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack itemInHand,
            float partialTick,
            float equipProgress,
            float swingProgress
    ) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        BayonetAnimationCurve.Sample animation = BayonetAnimationCurve.sample(swingProgress);
        float extension = animation.extension();
        float settleWobble = (float)Math.sin(animation.retract() * Math.PI * 3.0D)
                * (1.0F - animation.retract());
        poseStack.translate(
                side * (0.56F - extension * 0.10F),
                -0.52F - equipProgress * 0.60F - extension * 0.07F,
                -0.72F - extension * 0.58F
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(-16.0F - extension * 62.0F + settleWobble * 5.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (-12.0F + extension * 8.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * animation.orientationDegrees()));
        return true;
    }
}
