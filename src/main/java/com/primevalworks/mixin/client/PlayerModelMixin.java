package com.primevalworks.mixin.client;

import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void primevalworks$poseDinosaurRider(AvatarRenderState state, CallbackInfo callback) {
        PlayerModel model = (PlayerModel)(Object)this;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity rendered = minecraft.level.getEntity(state.id);
        if (rendered == null || !(rendered.getVehicle() instanceof FieldDodoEntity dinosaur)
                || !dinosaur.isSaddledMount()) return;

        float speed = dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS
                ? Mth.clamp(dinosaur.getSpinosaurusSwimSpeed() / 1.62F, 0.0F, 1.0F)
                : Mth.clamp(dinosaur.getPteranodonFlightSpeed() / 1.72F, 0.0F, 1.0F);
        float turn = Mth.clamp(Mth.wrapDegrees(dinosaur.yBodyRot - dinosaur.yBodyRotO) / 9.0F, -1.0F, 1.0F);
        float breathing = Mth.sin(state.ageInTicks * 0.18F) * 0.018F;
        if (dinosaur.getSpecies() == DinosaurSpecies.PTERANODON) {
            boolean airborne = dinosaur.isPteranodonAirborne();
            float pitch = airborne ? dinosaur.getXRot() * Mth.DEG_TO_RAD : 0.0F;
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float bank = airborne ? dinosaur.getPteranodonBankDegrees(partialTick) * Mth.DEG_TO_RAD : 0.0F;
            float flap = dinosaur.getPteranodonRiderFlap(state.ageInTicks);
            float animatedPitch = dinosaur.getPteranodonRiderBodyPitch(state.ageInTicks) * Mth.DEG_TO_RAD;
            float posture = dinosaur.getPteranodonRiderPosture(state.ageInTicks) * Mth.DEG_TO_RAD;
            float riderTurn = airborne ? turn : 0.0F;
            model.body.xRot = posture + (airborne ? pitch * 0.08F + animatedPitch * 0.12F
                    + flap * 0.022F : 0.0F) + breathing;
            model.body.zRot = bank * 0.08F;
            model.head.xRot -= model.body.xRot * 0.42F;
            model.head.zRot += bank * 0.05F;
            float gripPull = Mth.clamp(posture / 0.34F, -0.45F, 1.0F);
            model.rightArm.xRot = -1.18F - posture * 0.34F - pitch * 0.06F + flap * 0.026F + breathing;
            model.leftArm.xRot = -1.18F - posture * 0.34F - pitch * 0.06F + flap * 0.026F - breathing;
            model.rightArm.yRot = -0.32F - riderTurn * 0.08F - gripPull * 0.035F;
            model.leftArm.yRot = 0.32F - riderTurn * 0.08F + gripPull * 0.035F;
            model.rightArm.zRot = 0.12F + riderTurn * 0.10F + bank * 0.10F;
            model.leftArm.zRot = -0.12F + riderTurn * 0.10F + bank * 0.10F;
            model.rightLeg.xRot = -0.94F + pitch * 0.04F - speed * 0.06F - flap * 0.018F;
            model.leftLeg.xRot = -0.94F + pitch * 0.04F + speed * 0.06F - flap * 0.018F;
            model.rightLeg.yRot = 0.23F;
            model.leftLeg.yRot = -0.23F;
            model.rightLeg.zRot = 0.07F + turn * 0.06F + bank * 0.10F;
            model.leftLeg.zRot = -0.07F + turn * 0.06F + bank * 0.10F;
            return;
        }
        if (dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            boolean aquatic = dinosaur.isSpinosaurusSwimming() || dinosaur.isSpinosaurusBreaching();
            float pitch = aquatic
                    ? dinosaur.getXRot() * Mth.DEG_TO_RAD : 0.0F;
            float bank = dinosaur.getSpinosaurusBankDegrees(partialTick) * Mth.DEG_TO_RAD;
            float swim = Mth.sin(state.ageInTicks * (0.14F + speed * 0.16F));
            model.body.xRot = (aquatic ? -0.16F : 0.0F) + pitch * 0.10F
                    + swim * 0.018F + breathing;
            model.body.zRot = bank * 0.08F;
            model.head.xRot -= model.body.xRot * 0.36F;
            model.head.zRot += bank * 0.04F;
            model.rightArm.xRot = -1.34F - speed * 0.10F + swim * 0.025F;
            model.leftArm.xRot = -1.34F - speed * 0.10F - swim * 0.025F;
            model.rightArm.yRot = -0.38F - turn * 0.05F;
            model.leftArm.yRot = 0.38F - turn * 0.05F;
            model.rightArm.zRot = 0.14F + bank * 0.10F;
            model.leftArm.zRot = -0.14F + bank * 0.10F;
            model.rightLeg.xRot = -1.08F - speed * 0.10F;
            model.leftLeg.xRot = -1.08F + speed * 0.10F;
            model.rightLeg.yRot = 0.28F;
            model.leftLeg.yRot = -0.28F;
            model.rightLeg.zRot = 0.08F + bank * 0.08F;
            model.leftLeg.zRot = -0.08F + bank * 0.08F;
            return;
        }
        if (dinosaur.getSpecies() == DinosaurSpecies.TYRANNOSAURUS) {
            model.body.xRot = -0.06F + breathing;
            model.rightArm.xRot = -0.88F + breathing;
            model.leftArm.xRot = -0.88F - breathing;
            model.rightArm.yRot = -0.22F - turn * 0.06F;
            model.leftArm.yRot = 0.22F - turn * 0.06F;
            model.rightLeg.xRot = -1.18F - speed * 0.08F;
            model.leftLeg.xRot = -1.18F + speed * 0.08F;
            model.rightLeg.yRot = 0.27F;
            model.leftLeg.yRot = -0.27F;
            model.rightLeg.zRot = 0.08F;
            model.leftLeg.zRot = -0.08F;
        }
    }
}
