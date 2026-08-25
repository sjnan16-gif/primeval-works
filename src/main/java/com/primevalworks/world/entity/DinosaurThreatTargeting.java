package com.primevalworks.world.entity;

import com.primevalworks.config.PrimevalTuning;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class DinosaurThreatTargeting {
    private static final String INSTALLED_MARKER = "PrimevalDinosaurTargeting";

    private DinosaurThreatTargeting() {
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof Monster monster)
                || monster.getPersistentData().getBooleanOr(INSTALLED_MARKER, false)) {
            return;
        }
        monster.getPersistentData().putBoolean(INSTALLED_MARKER, true);
        monster.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                monster,
                FieldDodoEntity.class,
                1,
                false,
                false,
                (candidate, serverLevel) -> candidate instanceof FieldDodoEntity dinosaur
                        && PrimevalTuning.server().hostileMobTargeting()
                        && dinosaur.isAlive()
                        && !dinosaur.isOnExpedition()
        ));
    }
}
