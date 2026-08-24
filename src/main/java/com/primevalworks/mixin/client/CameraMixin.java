package com.primevalworks.mixin.client;

import com.primevalworks.client.render.entity.SpinosaurusRiderAttachment;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Entity entity;

    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void primevalworks$followAnimatedSpinosaurusHead(float partialTicks, CallbackInfo callback) {
        if (!(entity instanceof LocalPlayer player)
                || !Minecraft.getInstance().options.getCameraType().isFirstPerson()
                || !(player.getVehicle() instanceof FieldDodoEntity dinosaur)
                || dinosaur.getSpecies() != DinosaurSpecies.SPINOSAURUS
                || !dinosaur.isSaddledMount()) {
            return;
        }

        Vec3 riderOffset = SpinosaurusRiderAttachment.riderOffset(dinosaur, partialTicks);
        Vec3 mountPosition = new Vec3(
                Mth.lerp(partialTicks, dinosaur.xOld, dinosaur.getX()),
                Mth.lerp(partialTicks, dinosaur.yOld, dinosaur.getY()),
                Mth.lerp(partialTicks, dinosaur.zOld, dinosaur.getZ())
        );
        setPosition(mountPosition.add(riderOffset)
                .add(0.0D, player.getEyeHeight(), 0.0D));
    }
}
