package com.primevalworks.client.render.entity;

import net.minecraft.world.phys.Vec3;

import com.primevalworks.world.entity.FieldDodoEntity;

import java.util.HashMap;
import java.util.Map;

/** Shares the authored Spinosaurus seat socket between rider and first-person camera rendering. */
public final class SpinosaurusRiderAttachment {
    private static final Map<Integer, Sample> SAMPLES = new HashMap<>();

    private SpinosaurusRiderAttachment() {
    }

    public static void update(int entityId, Vec3 offsetFromEntityOrigin, boolean aquatic) {
        if (entityId < 0 || !finite(offsetFromEntityOrigin)) return;
        long now = System.nanoTime();
        Sample previous = SAMPLES.get(entityId);
        if (previous == null || previous.aquatic != aquatic
                || previous.offset.distanceToSqr(offsetFromEntityOrigin) > 9.0D) {
            SAMPLES.put(entityId, new Sample(offsetFromEntityOrigin, now, aquatic));
            return;
        }
        double deltaSeconds = Math.max(0.001D,
                Math.min(0.05D, (now - previous.updatedNanos) / 1_000_000_000.0D));
        Vec3 filtered = filter(previous.offset, offsetFromEntityOrigin, aquatic, deltaSeconds);
        SAMPLES.put(entityId, new Sample(filtered, now, aquatic));
    }

    public static Vec3 riderOffset(FieldDodoEntity dinosaur, float partialTick) {
        Vec3 fallback = dinosaur.getSpinosaurusPassengerOffset(partialTick);
        Vec3 animated = offset(dinosaur.getId());
        if (animated == null) return fallback;
        Vec3 adjusted = animated.add(0.0D, dinosaur.getSpinosaurusRiderVerticalAdjustment(), 0.0D);
        return adjusted.distanceToSqr(fallback) <= 12.0D ? adjusted : fallback;
    }

    static Vec3 filter(Vec3 previous, Vec3 target, boolean aquatic, double deltaSeconds) {
        return previous.lerp(target, MountAttachmentFilter.alpha(aquatic, deltaSeconds));
    }

    static Vec3 offset(int entityId) {
        Sample sample = SAMPLES.get(entityId);
        return sample == null ? null : sample.offset;
    }

    static void clear(int entityId) {
        SAMPLES.remove(entityId);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z)
                && value.lengthSqr() < 256.0D;
    }

    private record Sample(Vec3 offset, long updatedNanos, boolean aquatic) {
    }
}
