package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.CommandTableBlock;
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


public record CommandTableActionPayload(BlockPos tablePos, int action) implements CustomPacketPayload {
    public static final int RECALL_ALL = 0;
    public static final int STORE_ALL = 1;
    public static final Type<CommandTableActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "command_table_action")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CommandTableActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.tablePos.asLong());
                buffer.writeVarInt(payload.action);
            },
            buffer -> new CommandTableActionPayload(BlockPos.of(buffer.readLong()), buffer.readVarInt())
    );

    public static void handle(CommandTableActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> handleOnServer(player, payload));
    }

    private static void handleOnServer(ServerPlayer player, CommandTableActionPayload payload) {
        if (player.distanceToSqr(payload.tablePos.getCenter()) > 4096.0D
                || !player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())
                || !(player.level().getBlockEntity(payload.tablePos) instanceof CommandTableBlockEntity table)
                || !table.isOwnedBy(player.getUUID())) return;

        if (payload.action == STORE_ALL) {
            int stored = DinosaurOwnership.storeAllActive(player);
            player.sendOverlayMessage(Component.literal(stored == 0
                    ? "The active crew is already in the depot."
                    : stored + (stored == 1 ? " companion returned to the depot." : " companions returned to the depot.")));
            DinosaurRosterPayload.send(player, payload.tablePos);
            return;
        }
        if (payload.action != RECALL_ALL) return;

        int recalled = DinosaurOwnership.recallActive(player, payload.tablePos);
        player.sendOverlayMessage(Component.literal(
                recalled == 0 ? "No available companions answered the table."
                        : "The table recalled " + recalled + (recalled == 1 ? " companion." : " companions.")
        ));
        DinosaurRosterPayload.send(player, payload.tablePos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
