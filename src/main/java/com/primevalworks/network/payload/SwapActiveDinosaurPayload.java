package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record SwapActiveDinosaurPayload(BlockPos tablePos, UUID dinosaurId, int targetSlot)
        implements CustomPacketPayload {
    public static final Type<SwapActiveDinosaurPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "swap_active_dinosaur")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SwapActiveDinosaurPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.tablePos.asLong());
                buffer.writeUUID(payload.dinosaurId);
                buffer.writeVarInt(payload.targetSlot);
            },
            buffer -> new SwapActiveDinosaurPayload(BlockPos.of(buffer.readLong()), buffer.readUUID(), buffer.readVarInt())
    );

    public static void handle(SwapActiveDinosaurPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            if (payload.targetSlot < -1 || payload.targetSlot >= DinosaurOwnership.ACTIVE_LIMIT
                    || player.distanceToSqr(payload.tablePos.getCenter()) > 4096.0D
                    || !player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())
                    || !(player.level().getBlockEntity(payload.tablePos) instanceof CommandTableBlockEntity table)
                    || !table.isOwnedBy(player.getUUID())) return;
            DinosaurOwnership.SwapResult result = payload.targetSlot == -1
                    ? DinosaurOwnership.storeActive(player, payload.dinosaurId)
                    : DinosaurOwnership.swapIntoActive(player, payload.tablePos, payload.dinosaurId, payload.targetSlot);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(result.message()));
            DinosaurRosterPayload.send(player, payload.tablePos);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
