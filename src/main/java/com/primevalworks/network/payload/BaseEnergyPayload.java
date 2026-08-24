package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.CommandTableTestScreen;
import com.primevalworks.client.screen.EnergyNetworkScreen;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record BaseEnergyPayload(
        BlockPos tablePos,
        float stored,
        float capacity,
        float generationPerSecond,
        float consumptionPerSecond,
        int baseRadius,
        List<BlockPos> enabledConsumers
) implements CustomPacketPayload {
    private static final int MAX_CONSUMERS = 256;
    public static final Type<BaseEnergyPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "base_energy")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseEnergyPayload> STREAM_CODEC = StreamCodec.of(
            BaseEnergyPayload::encode,
            BaseEnergyPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, BaseEnergyPayload payload) {
        buffer.writeLong(payload.tablePos.asLong());
        buffer.writeFloat(payload.stored);
        buffer.writeFloat(payload.capacity);
        buffer.writeFloat(payload.generationPerSecond);
        buffer.writeFloat(payload.consumptionPerSecond);
        buffer.writeVarInt(payload.baseRadius);
        int size = Math.min(MAX_CONSUMERS, payload.enabledConsumers.size());
        buffer.writeVarInt(size);
        payload.enabledConsumers.stream().limit(size).forEach(pos -> buffer.writeLong(pos.asLong()));
    }

    private static BaseEnergyPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos tablePos = BlockPos.of(buffer.readLong());
        float stored = buffer.readFloat();
        float capacity = buffer.readFloat();
        float generation = buffer.readFloat();
        float consumption = buffer.readFloat();
        int baseRadius = Mth.clamp(buffer.readVarInt(), 8, 128);
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_CONSUMERS) throw new IllegalArgumentException("Invalid energy consumer count: " + size);
        List<BlockPos> consumers = new ArrayList<>(size);
        for (int index = 0; index < size; index++) consumers.add(BlockPos.of(buffer.readLong()));
        return new BaseEnergyPayload(tablePos, stored, capacity, generation, consumption, baseRadius,
                List.copyOf(consumers));
    }

    public static void send(ServerPlayer player, CommandTableBlockEntity table) {
        PacketDistributor.sendToPlayer(player, new BaseEnergyPayload(
                table.getBlockPos(),
                table.storedEnergy(),
                table.energyCapacity(),
                table.generationPerSecond(),
                table.consumptionPerSecond(),
                table.baseRadius(),
                List.copyOf(table.enabledEnergyConsumers())
        ));
    }

    public static void handle(BaseEnergyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            CommandTableTestScreen.acceptEnergyState(payload);
            EnergyNetworkScreen.acceptEnergyState(payload);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
