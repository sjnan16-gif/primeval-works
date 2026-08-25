package com.primevalworks.client.effect;

import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.SoundKeyframeData;
import com.primevalworks.config.PrimevalConfig;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.Map;
import java.util.WeakHashMap;

public final class DinosaurFootstepEffects {
    private static final double SHAKE_SECONDS = 0.09D;
    private static final Map<FieldDodoEntity, Long> LAST_CONTACT_NANOS = new WeakHashMap<>();
    private static long shakeStartedNanos;
    private static float shakeStrength;

    private DinosaurFootstepEffects() {
    }

    public static void onAnimationFootstep(KeyFrameEvent<FieldDodoEntity, SoundKeyframeData> event) {
        FieldDodoEntity dinosaur = event.animatable();
        if (!PrimevalConfig.CLIENT.heavyFootsteps.get()
                || !dinosaur.isAddedToLevel()
                || !dinosaur.isAlive()
                || dinosaur.isSilent()
                || !dinosaur.getSpecies().heavyweight()
                || !dinosaur.onGround()
                || dinosaur.getDeltaMovement().horizontalDistanceSqr() < 0.0004D) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || dinosaur.level() != minecraft.level) return;
        long now = System.nanoTime();
        long previous = LAST_CONTACT_NANOS.getOrDefault(dinosaur, 0L);
        if (now - previous < 30_000_000L) return;
        LAST_CONTACT_NANOS.put(dinosaur, now);

        Vec3 listener = minecraft.gameRenderer.getMainCamera().position();
        double range = PrimevalConfig.CLIENT.heavyFootstepRange.get();
        double distance = FootstepDistance.toBox(
                listener.x, listener.y, listener.z,
                dinosaur.getBoundingBox().minX, dinosaur.getBoundingBox().minY, dinosaur.getBoundingBox().minZ,
                dinosaur.getBoundingBox().maxX, dinosaur.getBoundingBox().maxY, dinosaur.getBoundingBox().maxZ
        );
        if (distance >= range) return;

        float proximity = 1.0F - Mth.clamp((float)(distance / range), 0.0F, 1.0F);
        float falloff = (float)Math.pow(proximity, 1.9D);
        float occlusion = isOccluded(minecraft, listener, dinosaur.getBoundingBox().getCenter()) ? 0.32F : 1.0F;
        boolean apex = dinosaur.getSpecies() == DinosaurSpecies.TYRANNOSAURUS
                || dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS;
        SoundEvent sound = apex ? SoundEvents.RAVAGER_STEP : SoundEvents.SNIFFER_STEP;
        float pitch = Mth.clamp(1.0F / Mth.sqrt(Math.max(0.65F, dinosaur.getGeneticScale())), 0.72F, 1.06F);
        float baseVolume = apex ? 1.55F : 1.18F;
        minecraft.level.playLocalSound(
                dinosaur.getX(), dinosaur.getY(), dinosaur.getZ(), sound, SoundSource.NEUTRAL,
                baseVolume * falloff * occlusion * PrimevalConfig.CLIENT.heavyFootstepVolume.get().floatValue(),
                pitch, false
        );

        boolean leftFoot = event.keyframeData().getSound().endsWith("left");
        if (PrimevalConfig.CLIENT.footstepDust.get()) {
            spawnSurfaceDust(minecraft, dinosaur, leftFoot, falloff);
        }
        boolean runningRex = dinosaur.getSpecies() == DinosaurSpecies.TYRANNOSAURUS
                && dinosaur.usesRunAnimation();
        boolean mountedSpino = dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS
                && dinosaur.getControllingPassenger() != null;
        if (minecraft.screen == null && (runningRex || mountedSpino)) {
            float speciesStrength = mountedSpino
                    ? dinosaur.isSpinosaurusLandSprinting() ? 1.85F : 0.92F
                    : 0.72F;
            shakeStartedNanos = now;
            shakeStrength = Math.max(shakeStrength, falloff * occlusion * speciesStrength
                    * PrimevalConfig.CLIENT.footstepShakeStrength.get().floatValue());
        }
    }

    public static void applyCameraImpulse(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || shakeStrength <= 0.0F) return;
        double age = (System.nanoTime() - shakeStartedNanos) / 1_000_000_000.0D;
        if (age < 0.0D || age >= SHAKE_SECONDS) {
            shakeStrength = 0.0F;
            return;
        }
        float accessibility = minecraft.options.screenEffectScale().get().floatValue();
        if (accessibility <= 0.0F) return;
        float phase = (float)(age / SHAKE_SECONDS);
        float impulse = shakeStrength * accessibility * (float)Math.pow(1.0F - phase, 3.0D);
        event.setPitch(event.getPitch() + Mth.sin(phase * Mth.TWO_PI - Mth.HALF_PI) * impulse * 0.72F);
        event.setRoll(event.getRoll() + Mth.sin(phase * Mth.TWO_PI) * impulse * 0.14F);
    }

    private static boolean isOccluded(Minecraft minecraft, Vec3 start, Vec3 end) {
        HitResult hit = minecraft.level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player
        ));
        return hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceToSqr(end) > 1.0D;
    }

    private static void spawnSurfaceDust(
            Minecraft minecraft,
            FieldDodoEntity dinosaur,
            boolean leftFoot,
            float falloff
    ) {
        BlockPos groundPos = dinosaur.getBlockPosBelowThatAffectsMyMovement();
        BlockState surface = minecraft.level.getBlockState(groundPos);
        if (surface.isAir()) return;
        double yaw = Math.toRadians(dinosaur.getYRot());
        double side = (leftFoot ? -0.58D : 0.58D) * dinosaur.getGeneticScale();
        double footX = dinosaur.getX() + Math.cos(yaw) * side;
        double footZ = dinosaur.getZ() + Math.sin(yaw) * side;
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, surface);
        int count = falloff > 0.3F ? 5 : 3;
        for (int index = 0; index < count; index++) {
            double spread = (index - (count - 1) * 0.5D) * 0.065D;
            minecraft.level.addParticle(
                    particle,
                    footX + Math.cos(yaw + Math.PI * 0.5D) * spread,
                    dinosaur.getY() + 0.08D,
                    footZ + Math.sin(yaw + Math.PI * 0.5D) * spread,
                    Math.cos(yaw + index) * 0.018D,
                    0.025D + index * 0.004D,
                    Math.sin(yaw + index) * 0.018D
            );
        }
    }
}
