package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleBaseEnergyConsumerPayload(BlockPos tablePos, BlockPos consumerPos)
        implements CustomPacketPayload {
    public static final Type<ToggleBaseEnergyConsumerPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "toggle_base_energy_consumer")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleBaseEnergyConsumerPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.tablePos.asLong());
                buffer.writeLong(payload.consumerPos.asLong());
            },
            buffer -> new ToggleBaseEnergyConsumerPayload(BlockPos.of(buffer.readLong()), BlockPos.of(buffer.readLong()))
    );

    public static void handle(ToggleBaseEnergyConsumerPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.tablePos.getCenter()) > 4096.0D
                || !player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())
                || !(player.level().getBlockEntity(payload.tablePos) instanceof CommandTableBlockEntity table)
                || !table.isOwnedBy(player.getUUID())
                || payload.consumerPos.distSqr(payload.tablePos) > (double)table.baseRadius() * table.baseRadius()
                || !player.level().isLoaded(payload.consumerPos)
                || BaseEnergyRules.demandPerSecond(player.level(), payload.consumerPos) <= 0) {
            return;
        }
        boolean enabled = table.toggleEnergyConsumer(player.level(), payload.consumerPos);
        player.sendOverlayMessage(Component.literal(enabled
                ? "Connected to the base energy network."
                : "Disconnected from the base energy network."));
        BaseEnergyPayload.send(player, table);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
