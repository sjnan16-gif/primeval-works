package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MountedDinosaurAttackPayload(int entityId, float lookYaw, float lookPitch) implements CustomPacketPayload {
    public static final Type<MountedDinosaurAttackPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "mounted_dinosaur_attack")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MountedDinosaurAttackPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.entityId);
                        buffer.writeFloat(payload.lookYaw);
                        buffer.writeFloat(payload.lookPitch);
                    },
                    buffer -> new MountedDinosaurAttackPayload(
                            buffer.readVarInt(), buffer.readFloat(), buffer.readFloat())
            );

    public static void handle(MountedDinosaurAttackPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.getVehicle() instanceof FieldDodoEntity dinosaur)
                || dinosaur.getId() != payload.entityId()) {
            return;
        }
        dinosaur.requestMountedAttack(player, payload.lookYaw, payload.lookPitch);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
