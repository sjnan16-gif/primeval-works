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

public record RecallDinosaurPayload(BlockPos tablePos, UUID dinosaurId) implements CustomPacketPayload {
    public static final Type<RecallDinosaurPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "recall_dinosaur")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RecallDinosaurPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.tablePos.asLong());
                buffer.writeUUID(payload.dinosaurId);
            },
            buffer -> new RecallDinosaurPayload(BlockPos.of(buffer.readLong()), buffer.readUUID())
    );

    public static void handle(RecallDinosaurPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            if (player.distanceToSqr(payload.tablePos.getCenter()) > 4096.0D
                    || !player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())
                    || !(player.level().getBlockEntity(payload.tablePos) instanceof CommandTableBlockEntity table)
                    || !table.isOwnedBy(player.getUUID())) return;
            DinosaurOwnership.SwapResult result = DinosaurOwnership.recallActive(
                    player, payload.tablePos, payload.dinosaurId);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(result.message()));
            DinosaurRosterPayload.send(player, payload.tablePos);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
