package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestBaseUpgradesPayload(BlockPos tablePos) implements CustomPacketPayload {
    public static final Type<RequestBaseUpgradesPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "request_base_upgrades")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBaseUpgradesPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeLong(payload.tablePos.asLong()),
            buffer -> new RequestBaseUpgradesPayload(BlockPos.of(buffer.readLong()))
    );

    public static void handle(RequestBaseUpgradesPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.tablePos.getCenter()) > 4096.0D
                || !player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())) {
            return;
        }
        CommandTableBlock.claimExisting(player, payload.tablePos);
        if (player.level().getBlockEntity(payload.tablePos) instanceof CommandTableBlockEntity table
                && table.isOwnedBy(player.getUUID())) {
            send(player, table, "Drag the branches to explore. Scroll to zoom.");
            DinosaurRosterPayload.send(player, payload.tablePos);
        }
    }

    public static void send(ServerPlayer player, CommandTableBlockEntity table, String notice) {
        PacketDistributor.sendToPlayer(player, new BaseUpgradesPayload(
                table.getBlockPos(), table.insight(), table.levels(), notice
        ));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
