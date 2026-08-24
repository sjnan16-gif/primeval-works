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

public record PurchaseBaseUpgradePayload(BlockPos tablePos, int upgradeId) implements CustomPacketPayload {
    public static final Type<PurchaseBaseUpgradePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "purchase_base_upgrade")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseBaseUpgradePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.tablePos.asLong());
                buffer.writeVarInt(payload.upgradeId);
            },
            buffer -> new PurchaseBaseUpgradePayload(BlockPos.of(buffer.readLong()), buffer.readVarInt())
    );

    public static void handle(PurchaseBaseUpgradePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.tablePos.getCenter()) > 4096.0D
                || !player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())
                || !(player.level().getBlockEntity(payload.tablePos) instanceof CommandTableBlockEntity table)) {
            return;
        }
        CommandTableBlockEntity.PurchaseResult result = table.purchase(player, payload.upgradeId);
        RequestBaseUpgradesPayload.send(player, table, result.message().getString());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
