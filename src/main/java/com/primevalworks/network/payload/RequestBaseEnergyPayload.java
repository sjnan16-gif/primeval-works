package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestBaseEnergyPayload(BlockPos tablePos) implements CustomPacketPayload {
    public static final Type<RequestBaseEnergyPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "request_base_energy")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBaseEnergyPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeLong(payload.tablePos.asLong()),
            buffer -> new RequestBaseEnergyPayload(BlockPos.of(buffer.readLong()))
    );

    public static void handle(RequestBaseEnergyPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.tablePos.getCenter()) > 4096.0D
                || !player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())
                || !(player.level().getBlockEntity(payload.tablePos) instanceof CommandTableBlockEntity table)
                || !table.isOwnedBy(player.getUUID())) return;
        BaseEnergyPayload.send(player, table);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
