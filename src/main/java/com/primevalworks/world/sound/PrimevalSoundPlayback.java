package com.primevalworks.world.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class PrimevalSoundPlayback {
    public static final double QUIET_RADIUS = 5.0D;
    public static final double SMALL_RADIUS = 6.0D;
    public static final double MACHINE_RADIUS = 8.0D;
    public static final double LARGE_RADIUS = 10.0D;

    private PrimevalSoundPlayback() {
    }

    public static void playAt(ServerLevel level, BlockPos pos, SoundEvent sound, SoundSource source,
                              float volume, float pitch, double radius) {
        playAt(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                sound, source, volume, pitch, radius);
    }

    public static void playAt(ServerLevel level, double x, double y, double z, SoundEvent sound,
                              SoundSource source, float volume, float pitch, double radius) {
        double clampedRadius = Mth.clamp(radius, QUIET_RADIUS, LARGE_RADIUS);
        double radiusSquared = clampedRadius * clampedRadius;
        ClientboundSoundPacket packet = new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source,
                x, y, z, volume, pitch, level.getRandom().nextLong());
        for (var player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= radiusSquared) player.connection.send(packet);
        }
    }

    public static void playFromEntity(Entity entity, SoundEvent sound, SoundSource source,
                                      float volume, float pitch, double radius) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        double clampedRadius = Mth.clamp(radius, QUIET_RADIUS, LARGE_RADIUS);
        double radiusSquared = clampedRadius * clampedRadius;
        ClientboundSoundEntityPacket packet = new ClientboundSoundEntityPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source,
                entity, volume, pitch, level.getRandom().nextLong());
        for (var player : level.players()) {
            if (player.distanceToSqr(entity) <= radiusSquared) player.connection.send(packet);
        }
    }

    public static void playLocalAt(Level level, double x, double y, double z, SoundEvent sound,
                                   SoundSource source, float volume, float pitch, double radius) {
        double clampedRadius = Mth.clamp(radius, QUIET_RADIUS, LARGE_RADIUS);
        double radiusSquared = clampedRadius * clampedRadius;
        Player localPlayer = level.players().stream().filter(Player::isLocalPlayer).findFirst().orElse(null);
        if (localPlayer == null || localPlayer.distanceToSqr(x, y, z) > radiusSquared) return;
        level.playLocalSound(x, y, z, sound, source, volume, pitch, false);
    }
}
