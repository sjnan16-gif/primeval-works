package com.primevalworks.world.damage;

import com.primevalworks.PrimevalWorks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

public final class PrimevalDamageTypes {
    public static final ResourceKey<DamageType> LASER_TURRET = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "laser_turret")
    );

    private PrimevalDamageTypes() {
    }

    public static DamageSource laserTurret(ServerLevel level, Vec3 sourcePosition) {
        return new DamageSource(
                level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LASER_TURRET),
                sourcePosition
        );
    }
}
