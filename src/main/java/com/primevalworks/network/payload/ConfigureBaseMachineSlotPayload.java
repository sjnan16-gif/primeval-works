package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.inventory.AutomationConfigurableContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ConfigureBaseMachineSlotPayload(BlockPos tablePos, BlockPos machinePos, int slot, boolean insert)
        implements CustomPacketPayload {
    public static final Type<ConfigureBaseMachineSlotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "configure_base_machine_slot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureBaseMachineSlotPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.tablePos.asLong());
                buffer.writeLong(payload.machinePos.asLong());
                buffer.writeVarInt(payload.slot);
                buffer.writeBoolean(payload.insert);
            },
            buffer -> new ConfigureBaseMachineSlotPayload(BlockPos.of(buffer.readLong()), BlockPos.of(buffer.readLong()),
                    buffer.readVarInt(), buffer.readBoolean())
    );

    public static void handle(ConfigureBaseMachineSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.tablePos.getCenter()) > 4096.0D
                || !player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())
                || !(player.level().getBlockEntity(payload.tablePos) instanceof CommandTableBlockEntity table)
                || !table.isOwnedBy(player.getUUID())
                || payload.machinePos.distSqr(payload.tablePos) > (double)table.baseRadius() * table.baseRadius()
                || !player.level().isLoaded(payload.machinePos)
                || !(player.level().getBlockEntity(payload.machinePos) instanceof Container container)
                || !(container instanceof AutomationConfigurableContainer configurable)
                || payload.slot < 0 || payload.slot >= container.getContainerSize()) return;

        if (payload.insert) configurable.toggleAutomationInsert(payload.slot);
        else configurable.toggleAutomationExtract(payload.slot);
        container.setChanged();
        BaseMachineRoutingPayload.send(player, payload.tablePos, payload.machinePos, container);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
