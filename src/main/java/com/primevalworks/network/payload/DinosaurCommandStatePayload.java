package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.CompanionScreen;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DinosaurCommandStatePayload(
        int entityId,
        int mode,
        int followers,
        int followerLimit,
        int baseRadius,
        String message
)
        implements CustomPacketPayload {
    public static final Type<DinosaurCommandStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dinosaur_command_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DinosaurCommandStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entityId);
                buffer.writeVarInt(payload.mode);
                buffer.writeVarInt(payload.followers);
                buffer.writeVarInt(payload.followerLimit);
                buffer.writeVarInt(payload.baseRadius);
                buffer.writeUtf(payload.message, 160);
            },
            buffer -> new DinosaurCommandStatePayload(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(160)));

    public static DinosaurCommandStatePayload from(ServerPlayer player, FieldDodoEntity dinosaur, String message) {
        int baseRadius = dinosaur.getCommandTablePos()
                .map(pos -> CommandTableBlock.baseRadius(player.level(), pos))
                .orElse(50);
        return new DinosaurCommandStatePayload(dinosaur.getId(), dinosaur.getCommandMode().ordinal(),
                DinosaurOwnership.followerCount(player), DinosaurOwnership.followerLimit(player), baseRadius,
                message == null ? "" : message);
    }

    public static void handle(DinosaurCommandStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CompanionScreen.acceptCommandState(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
