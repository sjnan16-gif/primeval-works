package com.primevalworks.client.effect;

import com.mojang.math.Axis;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public final class PteranodonFirstPersonPose {
    private PteranodonFirstPersonPose() {
    }

    public static void apply(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof FieldDodoEntity dinosaur)
                || !dinosaur.isSaddledMount()) {
            return;
        }

        if (dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS) {
            float speed = Mth.clamp(dinosaur.getSpinosaurusSwimSpeed() / 1.62F, 0.0F, 1.0F);
            float side = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? 1.0F : -1.0F;
            float swim = Mth.sin((dinosaur.tickCount + event.getPartialTick()) * (0.14F + speed * 0.16F));
            float bank = dinosaur.getSpinosaurusBankDegrees(event.getPartialTick());
            event.getPoseStack().translate(-side * (0.062F + speed * 0.018F),
                    0.105F + swim * 0.008F, -0.15F - speed * 0.035F);
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-13.0F - speed * 5.0F));
            event.getPoseStack().mulPose(Axis.YP.rotationDegrees(side * 7.0F));
            event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(side * -5.0F + bank * 0.08F));
            return;
        }
        if (dinosaur.getSpecies() != DinosaurSpecies.PTERANODON) return;

        float age = dinosaur.tickCount + event.getPartialTick();
        float posture = dinosaur.getPteranodonRiderPosture(age);
        float flap = dinosaur.getPteranodonRiderFlap(age);
        float bank = dinosaur.isPteranodonAirborne()
                ? dinosaur.getPteranodonBankDegrees(event.getPartialTick())
                : 0.0F;
        float side = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? 1.0F : -1.0F;
        float grip = Mth.clamp(Math.abs(posture) / 17.0F, 0.0F, 1.0F);

        event.getPoseStack().translate(-side * (0.055F + grip * 0.025F),
                0.08F + grip * 0.035F - flap * flap * 0.012F,
                -0.10F - grip * 0.045F);
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-8.0F - posture * 0.34F + flap * 1.2F));
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(side * (5.0F + grip * 3.0F)));
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(side * -4.0F + bank * 0.10F));
    }
}
