package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.CommandTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClaimCommandTablePayload(BlockPos tablePos) implements CustomPacketPayload {
    public static final Type<ClaimCommandTablePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "claim_command_table")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimCommandTablePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ClaimCommandTablePayload::tablePos,
            ClaimCommandTablePayload::new
    );

    public static void handle(ClaimCommandTablePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.distanceToSqr(payload.tablePos.getCenter()) <= 64.0D
                && player.level().getBlockState(payload.tablePos).is(ModBlocks.COMMAND_TABLE.get())) {
            CommandTableBlock.claimExisting(player, payload.tablePos);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
