package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.work.BaseInventoryIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestBaseInventoryPayload(BlockPos commandTablePos) implements CustomPacketPayload {
    public static final Type<RequestBaseInventoryPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "request_base_inventory")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBaseInventoryPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeLong(payload.commandTablePos.asLong()),
            buffer -> new RequestBaseInventoryPayload(BlockPos.of(buffer.readLong()))
    );

    public static void handle(RequestBaseInventoryPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.commandTablePos.getCenter()) > 4096.0D) return;
        var table = CommandTableBlock.tableEntity(player.level(), payload.commandTablePos);
        if (table == null || !table.isOwnedBy(player.getUUID())) return;

        var entries = BaseInventoryIndex.scan(player.level(), payload.commandTablePos, table.baseRadius()).stream()
                .map(indexed -> new BaseInventoryPayload.ContainerEntry(
                        indexed.pos(), indexed.acceptsAnyItem(), indexed.canSupplyItems(), indexed.canReceiveItems(),
                        indexed.items().stream()
                        .map(item -> new BaseInventoryPayload.ItemEntry(
                                item.identifier().toString(), item.extractableCount(), item.acceptsMore()))
                        .toList()))
                .toList();
        PacketDistributor.sendToPlayer(player, new BaseInventoryPayload(payload.commandTablePos, entries));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
