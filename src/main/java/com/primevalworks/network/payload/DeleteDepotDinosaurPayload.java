package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record DeleteDepotDinosaurPayload(BlockPos tablePos, UUID dinosaurId)
        implements CustomPacketPayload {
    public static final Type<DeleteDepotDinosaurPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "delete_depot_dinosaur")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteDepotDinosaurPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.tablePos.asLong());
                buffer.writeUUID(payload.dinosaurId);
            },
            buffer -> new DeleteDepotDinosaurPayload(BlockPos.of(buffer.readLong()), buffer.readUUID())
    );

    public static void handle(DeleteDepotDinosaurPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            if (player.distanceToSqr(payload.tablePos.getCenter()) > 4096.0D
                    || !player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())
                    || !(player.level().getBlockEntity(payload.tablePos) instanceof CommandTableBlockEntity table)
                    || !table.isOwnedBy(player.getUUID())) return;
            DinosaurOwnership.SwapResult result = DinosaurOwnership.deleteFromDepot(player, payload.dinosaurId);
            player.sendOverlayMessage(Component.literal(result.message()));
            DinosaurRosterPayload.send(player, payload.tablePos);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
