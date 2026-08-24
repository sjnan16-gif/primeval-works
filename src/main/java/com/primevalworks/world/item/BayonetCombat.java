package com.primevalworks.world.item;

import com.primevalworks.registry.ModItems;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.Comparator;

public final class BayonetCombat {
    public static final float MIN_RANGE = 1.2F;
    public static final float MAX_RANGE = 2.2F;
    private static final float SECONDARY_DAMAGE = 4.5F;
    private static final double MIN_FORWARD_DOT = 0.84D;

    private BayonetCombat() {
    }

    public static void onAttack(AttackEntityEvent event) {
        ItemStack weapon = event.getEntity().getMainHandItem();
        if (!weapon.is(ModItems.ANCIENT_REFORGED_BAYONET.get())) return;
        if (!event.getEntity().isWithinAttackRange(
                weapon,
                event.getTarget().getBoundingBox(),
                0.0D
        )) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTarget() instanceof LivingEntity primary) {
            strikeSecondaryTargets(player, primary, weapon);
        }
    }

    static void strikeSecondaryTargets(ServerPlayer player, LivingEntity primary, ItemStack weapon) {
        ServerLevel level = player.level();
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        float damage = SECONDARY_DAMAGE * player.getAttackStrengthScale(0.5F);
        level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(MAX_RANGE + 1.0D),
                        candidate -> candidate != player
                                && candidate != primary
                                && candidate.isAlive()
                                && !candidate.isAlliedTo(player)
                                && (!(candidate instanceof FieldDodoEntity dinosaur)
                                        || !dinosaur.isOwnedBy(player.getUUID()))
                                && player.isWithinAttackRange(weapon, candidate.getBoundingBox(), 0.0D)
                                && player.hasLineOfSight(candidate)
                                && insidePiercingArc(origin, look, candidate)
                ).stream()
                .sorted(Comparator.comparingDouble(candidate -> candidate.getBoundingBox().distanceToSqr(origin)))
                .limit(3)
                .forEach(candidate -> candidate.hurtServer(
                        level,
                        weapon.getDamageSource(player, () -> player.damageSources().playerAttack(player)),
                        damage
                ));

        Vec3 effect = origin.add(look.scale(1.7D));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                effect.x, effect.y - 0.2D, effect.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    static boolean insidePiercingArc(Vec3 origin, Vec3 look, LivingEntity target) {
        Vec3 direction = target.getBoundingBox().getCenter().subtract(origin);
        double length = direction.length();
        return length > 1.0E-5D && look.dot(direction.scale(1.0D / length)) >= MIN_FORWARD_DOT;
    }
}
