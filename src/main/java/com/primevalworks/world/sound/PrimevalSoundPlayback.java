package com.primevalworks.world.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class PrimevalSoundPlayback {
    private static final double MINIMUM_RADIUS = 5.0D;
    public static final double MACHINE_RADIUS = 8.0D;
    public static final double LARGE_RADIUS = 10.0D;

    private PrimevalSoundPlayback() {
    }

    public static void playFromEntity(Entity entity, SoundEvent sound, SoundSource source,
                                      float volume, float pitch, double radius) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        double clampedRadius = Mth.clamp(radius, MINIMUM_RADIUS, LARGE_RADIUS);
        double radiusSquared = clampedRadius * clampedRadius;
        ClientboundSoundEntityPacket packet = new ClientboundSoundEntityPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source,
                entity, volume, pitch, level.getRandom().nextLong());
        for (var player : level.players()) {
            if (player.distanceToSqr(entity) <= radiusSquared) player.connection.send(packet);
        }
    }
}
