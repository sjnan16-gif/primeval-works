package com.primevalworks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.primevalworks.client.render.entity.SpinosaurusRiderAttachment;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void primevalworks$followAnimatedSpinosaurusHead(
            AvatarRenderState state,
            PoseStack poseStack,
            float bodyRot,
            float entityScale,
            CallbackInfo callback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity rendered = minecraft.level.getEntity(state.id);
        if (rendered == null
                || !(rendered.getVehicle() instanceof FieldDodoEntity dinosaur)
                || dinosaur.getSpecies() != DinosaurSpecies.SPINOSAURUS
                || !dinosaur.isSaddledMount()) return;

        Vec3 riderOffset = SpinosaurusRiderAttachment.riderOffset(dinosaur, state.partialTick);
        double mountX = Mth.lerp(state.partialTick, dinosaur.xOld, dinosaur.getX());
        double mountY = Mth.lerp(state.partialTick, dinosaur.yOld, dinosaur.getY());
        double mountZ = Mth.lerp(state.partialTick, dinosaur.zOld, dinosaur.getZ());
        Vec3 desired = new Vec3(mountX, mountY, mountZ).add(riderOffset);
        Vec3 correction = desired.subtract(state.x, state.y, state.z);
        if (correction.lengthSqr() <= 16.0D) {
            poseStack.translate(correction.x, correction.y, correction.z);
        }
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void primevalworks$followPteranodonBody(
            AvatarRenderState state,
            PoseStack poseStack,
            float bodyRot,
            float entityScale,
            CallbackInfo callback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity rendered = minecraft.level.getEntity(state.id);
        if (rendered == null
                || !(rendered.getVehicle() instanceof FieldDodoEntity dinosaur)
                || !dinosaur.isSaddledMount()
                || !(dinosaur.getSpecies() == DinosaurSpecies.PTERANODON && dinosaur.isPteranodonAirborne()
                || dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS)) {
            return;
        }

        float pitch = Mth.lerp(state.partialTick, dinosaur.xRotO, dinosaur.getXRot());
        if (dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            float bank = dinosaur.getSpinosaurusBankDegrees(state.partialTick);
            float speed = Mth.clamp(dinosaur.getSpinosaurusSwimSpeed() / 1.62F, 0.0F, 1.0F);
            float swimBob = Mth.sin(state.ageInTicks * (0.13F + speed * 0.12F)) * (0.018F + speed * 0.025F);
            poseStack.translate(0.0F, 0.86F + swimBob, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.clamp(-pitch, -58.0F, 58.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.clamp(bank, -27.0F, 27.0F)));
            poseStack.translate(0.0F, -0.86F, 0.0F);
            return;
        }
        float bank = dinosaur.getPteranodonBankDegrees(state.partialTick);
        float rootPitch = dinosaur.getPteranodonRiderRootPitch(state.ageInTicks);
        float animatedPitch = dinosaur.getPteranodonRiderBodyPitch(state.ageInTicks);
        float bob = dinosaur.getPteranodonRiderBob(state.ageInTicks);
        float swayPitch = dinosaur.getPteranodonRideSwayPitch(state.ageInTicks);
        float swayRoll = dinosaur.getPteranodonRideSwayRoll(state.ageInTicks);
        poseStack.translate(0.0F, bob, 0.0F);
        poseStack.translate(0.0F, 0.74F, 0.0F);
        float attachedPitch = Mth.clamp(
                -pitch + rootPitch + animatedPitch * 0.66F + swayPitch,
                -48.0F,
                48.0F
        );
        float attachedRoll = Mth.clamp(bank + swayRoll, -32.0F, 32.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(attachedPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(attachedRoll));
        poseStack.translate(0.0F, -0.74F, 0.0F);
    }
}
