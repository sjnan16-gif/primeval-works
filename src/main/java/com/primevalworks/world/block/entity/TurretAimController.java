package com.primevalworks.world.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class TurretAimController {
    private int targetEntityId = -1;
    private float yaw;
    private float previousYaw;
    private float pitch;
    private float previousPitch;

    public int targetEntityId() {
        return targetEntityId;
    }

    public boolean setTargetEntityId(int targetEntityId) {
        if (this.targetEntityId == targetEntityId) return false;
        this.targetEntityId = targetEntityId;
        return true;
    }

    public void clientTick(Level level, BlockPos pos, double pivotHeight) {
        previousYaw = yaw;
        previousPitch = pitch;
        LivingEntity target = target(level);
        float desiredYaw = 0.0F;
        float desiredPitch = 0.0F;
        if (target != null) {
            Vec3 origin = Vec3.atCenterOf(pos).add(0.0D, pivotHeight - 0.5D, 0.0D);
            Vec3 direction = target.getEyePosition().subtract(origin);
            desiredYaw = (float)(Mth.atan2(-direction.x, -direction.z) * Mth.RAD_TO_DEG);
            desiredPitch = (float)(Mth.atan2(direction.y,
                    Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * Mth.RAD_TO_DEG);
        }
        yaw = Mth.approachDegrees(yaw, desiredYaw, target == null ? 3.25F : 7.5F);
        pitch = Mth.lerp(target == null ? 0.14F : 0.24F, pitch, desiredPitch);
    }

    public float yaw(float partialTick) {
        return Mth.rotLerp(partialTick, previousYaw, yaw);
    }

    public float pitch(float partialTick) {
        return Mth.lerp(partialTick, previousPitch, pitch);
    }

    public @Nullable LivingEntity target(Level level) {
        if (targetEntityId < 0) return null;
        Entity entity = level.getEntity(targetEntityId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }
}
