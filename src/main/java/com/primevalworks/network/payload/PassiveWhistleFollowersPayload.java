package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.DinoWhistleScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PassiveWhistleFollowersPayload(int inventorySlot, List<Entry> entries)
        implements CustomPacketPayload {
    public static final Type<PassiveWhistleFollowersPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "passive_whistle_followers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PassiveWhistleFollowersPayload> STREAM_CODEC =
            StreamCodec.of(PassiveWhistleFollowersPayload::encode, PassiveWhistleFollowersPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, PassiveWhistleFollowersPayload payload) {
        buffer.writeVarInt(payload.inventorySlot);
        buffer.writeVarInt(Math.min(3, payload.entries.size()));
        for (Entry entry : payload.entries.stream().limit(3).toList()) {
            buffer.writeVarInt(entry.entityId);
            buffer.writeUUID(entry.uuid);
            buffer.writeUtf(entry.name, 96);
            buffer.writeVarInt(entry.rating);
            buffer.writeBoolean(entry.compatible);
            buffer.writeBoolean(entry.assigned);
        }
    }

    private static PassiveWhistleFollowersPayload decode(RegistryFriendlyByteBuf buffer) {
        int inventorySlot = buffer.readVarInt();
        int count = Math.min(3, buffer.readVarInt());
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new Entry(buffer.readVarInt(), buffer.readUUID(), buffer.readUtf(96),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean()));
        }
        return new PassiveWhistleFollowersPayload(inventorySlot, List.copyOf(entries));
    }

    public static void handle(PassiveWhistleFollowersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> DinoWhistleScreen.acceptFollowers(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(int entityId, UUID uuid, String name, int rating, boolean compatible, boolean assigned) {}
}
