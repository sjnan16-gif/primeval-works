package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.work.DinosaurCommandMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DinosaurCommandPayload(int entityId, int requestedMode) implements CustomPacketPayload {
    public static final Type<DinosaurCommandPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dinosaur_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DinosaurCommandPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entityId);
                buffer.writeVarInt(payload.requestedMode);
            },
            buffer -> new DinosaurCommandPayload(buffer.readVarInt(), buffer.readVarInt()));

    public static void handle(DinosaurCommandPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            if (!(player.level().getEntity(payload.entityId) instanceof FieldDodoEntity dinosaur)
                    || !dinosaur.isOwnedBy(player.getUUID())
                    || player.distanceToSqr(dinosaur) > 64.0D * 64.0D) return;
            String message = "";
            if (payload.requestedMode >= 0 && payload.requestedMode < DinosaurCommandMode.values().length) {
                DinosaurOwnership.SwapResult result = DinosaurOwnership.setCommandMode(
                        player, dinosaur, DinosaurCommandMode.byId(payload.requestedMode));
                message = result.message();
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(message));
            }
            PacketDistributor.sendToPlayer(player, DinosaurCommandStatePayload.from(player, dinosaur, message));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
