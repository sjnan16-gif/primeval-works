package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.WhistleFollowerPickerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record WhistleFollowerListPayload(BlockPos first, BlockPos second, boolean hasSecond,
                                         int mode, int pattern, int range,
                                         long selectionToken,
                                         List<Entry> entries) implements CustomPacketPayload {
    public static final Type<WhistleFollowerListPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "whistle_follower_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WhistleFollowerListPayload> STREAM_CODEC = StreamCodec.of(
            WhistleFollowerListPayload::encode, WhistleFollowerListPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, WhistleFollowerListPayload payload) {
        buffer.writeLong(payload.first.asLong());
        buffer.writeBoolean(payload.hasSecond);
        if (payload.hasSecond) buffer.writeLong(payload.second.asLong());
        buffer.writeVarInt(payload.mode);
        buffer.writeVarInt(payload.pattern);
        buffer.writeVarInt(payload.range);
        buffer.writeLong(payload.selectionToken);
        buffer.writeVarInt(Math.min(3, payload.entries.size()));
        for (Entry entry : payload.entries.stream().limit(3).toList()) {
            buffer.writeVarInt(entry.entityId);
            buffer.writeUUID(entry.uuid);
            buffer.writeUtf(entry.name, 96);
            buffer.writeUtf(entry.species, 48);
            buffer.writeVarInt(entry.level);
            buffer.writeVarInt(entry.rating);
            buffer.writeBoolean(entry.compatible);
        }
    }

    private static WhistleFollowerListPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos first = BlockPos.of(buffer.readLong());
        boolean hasSecond = buffer.readBoolean();
        BlockPos second = hasSecond ? BlockPos.of(buffer.readLong()) : first;
        int mode = buffer.readVarInt();
        int pattern = buffer.readVarInt();
        int range = buffer.readVarInt();
        long selectionToken = buffer.readLong();
        int count = Math.min(3, buffer.readVarInt());
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new Entry(buffer.readVarInt(), buffer.readUUID(), buffer.readUtf(96),
                    buffer.readUtf(48), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()));
        }
        return new WhistleFollowerListPayload(first, second, hasSecond, mode, pattern,
                range, selectionToken, List.copyOf(entries));
    }

    public static void handle(WhistleFollowerListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WhistleFollowerPickerScreen.open(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Entry(int entityId, UUID uuid, String name, String species,
                        int level, int rating, boolean compatible) {}
}
