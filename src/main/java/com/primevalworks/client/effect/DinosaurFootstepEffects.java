package com.primevalworks.client.effect;

import com.primevalworks.registry.ModEntities;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.entity.DinosaurSpecies;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DinosaurFootstepEffects {
    private static final int RUN_CYCLE_TICKS = 10;
    private static final double MAX_EFFECT_DISTANCE = 24.0D;
    private static final double SHAKE_SECONDS = 0.09D;
    private static final Map<Integer, RunGait> RUN_GAITS = new HashMap<>();
    private static long shakeStartedNanos;
    private static float shakeStrength;

    private DinosaurFootstepEffects() {
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            RUN_GAITS.clear();
            shakeStrength = 0.0F;
            return;
        }
        Set<Integer> presentRexes = new HashSet<>();
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof FieldDodoEntity dinosaur)) continue;
            boolean tyrannosaurus = dinosaur.getType() == ModEntities.TYRANNOSAURUS.get();
            boolean mountedSpinosaurusLand = dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS
                    && dinosaur.getControllingPassenger() != null
                    && !dinosaur.isSpinosaurusAquaticPose();
            if (!tyrannosaurus && !mountedSpinosaurusLand) continue;
            presentRexes.add(dinosaur.getId());
            RunGait gait = RUN_GAITS.computeIfAbsent(dinosaur.getId(), ignored -> new RunGait());
            boolean running = dinosaur.onGround()
                    && (dinosaur.usesRunAnimation() || mountedSpinosaurusLand)
                    && dinosaur.getDeltaMovement().horizontalDistanceSqr() >= 0.0025D
                    && dinosaur.getWorkAction() == 0;
            if (!running) {
                gait.running = false;
                gait.cycleTick = 0;
                continue;
            }
            if (!gait.running) {
                gait.running = true;
                gait.cycleTick = 0;
                emitFootContact(minecraft, dinosaur, false);
                continue;
            }
            int cycleTicks = mountedSpinosaurusLand
                    ? Mth.clamp(Math.round(13.0F / Mth.clamp(dinosaur.walkAnimation.speed(), 0.72F, 1.75F)), 7, 15)
                    : RUN_CYCLE_TICKS;
            int secondContact = Math.max(2, cycleTicks / 2);
            gait.cycleTick = (gait.cycleTick + 1) % cycleTicks;
            if (gait.cycleTick == secondContact) {
                emitFootContact(minecraft, dinosaur, true);
            } else if (gait.cycleTick == 0) {
                emitFootContact(minecraft, dinosaur, false);
            }
        }
        RUN_GAITS.keySet().removeIf(id -> !presentRexes.contains(id));
    }

    public static void applyCameraImpulse(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || shakeStrength <= 0.0F) {
            return;
        }
        double age = (System.nanoTime() - shakeStartedNanos) / 1_000_000_000.0D;
        if (age < 0.0D || age >= SHAKE_SECONDS) {
            shakeStrength = 0.0F;
            return;
        }
        float accessibility = minecraft.options.screenEffectScale().get().floatValue();
        if (accessibility <= 0.0F) {
            return;
        }
        float phase = (float) (age / SHAKE_SECONDS);
        float envelope = (float) Math.pow(1.0F - phase, 3.0D);
        float impulse = shakeStrength * accessibility * envelope;
        float impactWave = Mth.sin(phase * Mth.TWO_PI - Mth.HALF_PI);
        event.setPitch(event.getPitch() + impactWave * impulse * 0.72F);
        event.setRoll(event.getRoll() + Mth.sin(phase * Mth.TWO_PI) * impulse * 0.14F);
    }

    private static void emitFootContact(Minecraft minecraft, FieldDodoEntity dinosaur, boolean leftFoot) {
        Vec3 listener = minecraft.gameRenderer.getMainCamera().position();
        double distance = listener.distanceTo(dinosaur.position());
        if (distance > MAX_EFFECT_DISTANCE) {
            return;
        }
        float falloff = 1.0F - Mth.clamp((float) ((distance - 2.0D) / (MAX_EFFECT_DISTANCE - 2.0D)), 0.0F, 1.0F);
        falloff *= falloff;
        float occlusion = isOccluded(minecraft, listener, dinosaur.getBoundingBox().getCenter()) ? 0.38F : 1.0F;
        if (minecraft.screen == null) {
            shakeStartedNanos = System.nanoTime();
            float speciesStrength = dinosaur.getSpecies() == DinosaurSpecies.SPINOSAURUS
                    ? dinosaur.isSpinosaurusLandSprinting() ? 1.85F : 0.92F
                    : 0.72F;
            shakeStrength = Math.max(shakeStrength, falloff * occlusion * speciesStrength);
        }
        spawnSurfaceDust(minecraft, dinosaur, leftFoot, falloff);
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
        if (surface.isAir()) {
            return;
        }
        double yaw = Math.toRadians(dinosaur.getYRot());
        double side = leftFoot ? -0.58D : 0.58D;
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

    private static final class RunGait {
        private boolean running;
        private int cycleTick;
    }
}
