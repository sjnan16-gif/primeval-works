package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.CommandTableTestScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record BaseUpgradesPayload(BlockPos tablePos, int insight, List<Integer> levels, String notice)
        implements CustomPacketPayload {
    private static final int MAX_UPGRADES = 32;
    public static final Type<BaseUpgradesPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "base_upgrades")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseUpgradesPayload> STREAM_CODEC = StreamCodec.of(
            BaseUpgradesPayload::encode,
            BaseUpgradesPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, BaseUpgradesPayload payload) {
        buffer.writeLong(payload.tablePos.asLong());
        buffer.writeVarInt(payload.insight);
        int size = Math.min(MAX_UPGRADES, payload.levels.size());
        buffer.writeVarInt(size);
        payload.levels.stream().limit(size).forEach(buffer::writeVarInt);
        buffer.writeUtf(payload.notice, 256);
    }

    private static BaseUpgradesPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos tablePos = BlockPos.of(buffer.readLong());
        int insight = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_UPGRADES) {
            throw new IllegalArgumentException("Invalid base-upgrade count: " + size);
        }
        List<Integer> levels = new ArrayList<>(size);
        for (int index = 0; index < size; index++) levels.add(buffer.readVarInt());
        return new BaseUpgradesPayload(tablePos, insight, List.copyOf(levels), buffer.readUtf(256));
    }

    public static void handle(BaseUpgradesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CommandTableTestScreen.acceptBaseState(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
