package com.primevalworks.world.entity;

import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

public final class DinosaurTargetProtection {
    private DinosaurTargetProtection() {
    }

    public static void preventHostileTargeting(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Enemy
                && event.getNewAboutToBeSetTarget() instanceof FieldDodoEntity) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
}
