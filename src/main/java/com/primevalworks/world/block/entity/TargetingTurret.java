package com.primevalworks.world.block.entity;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public interface TargetingTurret extends ActiveEnergyConsumer {
    TurretAimController aimController();

    default float aimYaw(float partialTick) {
        return aimController().yaw(partialTick);
    }

    default float aimPitch(float partialTick) {
        return aimController().pitch(partialTick);
    }

    default @Nullable LivingEntity renderTarget(Level level) {
        return aimController().target(level);
    }

    @Override
    default boolean requestsBaseEnergy(Level level) {
        return renderTarget(level) != null;
    }
}
