package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestDinosaurWorkStatePayload(int entityId, BlockPos commandTablePos)
        implements CustomPacketPayload {
    public static final Type<RequestDinosaurWorkStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "request_dinosaur_work_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestDinosaurWorkStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entityId);
                buffer.writeLong(payload.commandTablePos.asLong());
            },
            buffer -> new RequestDinosaurWorkStatePayload(buffer.readVarInt(), BlockPos.of(buffer.readLong()))
    );

    public static void handle(RequestDinosaurWorkStatePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.commandTablePos.getCenter()) > 4096.0D
                || !(player.level().getEntity(payload.entityId) instanceof FieldDodoEntity dinosaur)
                || !dinosaur.isAlive()
                || !dinosaur.isOwnedBy(player.getUUID())
                || dinosaur.getCommandTablePos().filter(payload.commandTablePos::equals).isEmpty()) return;
        var table = CommandTableBlock.tableEntity(player.level(), payload.commandTablePos);
        if (table == null || !table.isOwnedBy(player.getUUID())) return;
        PacketDistributor.sendToPlayer(player, DinosaurWorkStatePayload.from(dinosaur));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
