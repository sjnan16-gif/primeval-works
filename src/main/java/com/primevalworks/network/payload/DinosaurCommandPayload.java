package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.work.DinosaurCommandMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DinosaurCommandPayload(int entityId, int requestedMode) implements CustomPacketPayload {
    public static final int RECALL_HOME = 100;
    public static final Type<DinosaurCommandPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dinosaur_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DinosaurCommandPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entityId);
                buffer.writeVarInt(payload.requestedMode);
            },
            buffer -> new DinosaurCommandPayload(buffer.readVarInt(), buffer.readVarInt()));

    public static void handle(DinosaurCommandPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            if (!(player.level().getEntity(payload.entityId) instanceof FieldDodoEntity dinosaur)
                    || !dinosaur.isOwnedBy(player.getUUID())) return;
            String message = "";
            boolean recall = payload.requestedMode == RECALL_HOME;
            if (recall ? !canRecall(player, dinosaur) : player.distanceToSqr(dinosaur) > 64.0D * 64.0D) {
                return;
            }
            int modeId = recall ? DinosaurCommandMode.HOME.ordinal() : payload.requestedMode;
            if (modeId >= 0 && modeId < DinosaurCommandMode.values().length) {
                DinosaurCommandMode mode = DinosaurCommandMode.byId(modeId);
                DinosaurOwnership.SwapResult result = DinosaurOwnership.setCommandMode(player, dinosaur, mode);
                if (result.success() && recall) {
                    result = dinosaur.getCommandTablePos()
                            .map(tablePos -> DinosaurOwnership.recallActive(player, tablePos, dinosaur.getUUID()))
                            .orElse(result);
                }
                message = result.message();
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(message));
            }
            PacketDistributor.sendToPlayer(player, DinosaurCommandStatePayload.from(player, dinosaur, message));
        });
    }

    private static boolean canRecall(ServerPlayer player, FieldDodoEntity dinosaur) {
        if (!DinosaurOwnership.activeIds(player).contains(dinosaur.getUUID())) return false;
        CommandTableBlock.ClaimedTable table = CommandTableBlock.getClaimedTable(player).orElse(null);
        if (table == null || table.level() != player.level()
                || dinosaur.getCommandTablePos().filter(table.pos()::equals).isEmpty()) {
            return false;
        }
        double interactionRangeSquared = 64.0D * 64.0D;
        return player.distanceToSqr(dinosaur) <= interactionRangeSquared
                || player.distanceToSqr(table.pos().getCenter()) <= interactionRangeSquared;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
