package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StopWhistleFieldWorkPayload(int inventorySlot, int mode) implements CustomPacketPayload {
    public static final Type<StopWhistleFieldWorkPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "stop_whistle_field_work"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StopWhistleFieldWorkPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeVarInt(payload.inventorySlot);
                buffer.writeVarInt(payload.mode);
            }, buffer -> new StopWhistleFieldWorkPayload(buffer.readVarInt(), buffer.readVarInt()));

    public static void handle(StopWhistleFieldWorkPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            int stopped = apply(player, payload);
            DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.byId(payload.mode);
            player.sendOverlayMessage(Component.literal(stopped == 0
                    ? "No active " + mode.title().toLowerCase() + " order to stop."
                    : "Stopped " + stopped + " " + mode.title().toLowerCase()
                    + (stopped == 1 ? " order." : " orders.")));
            PassiveWhistleFollowersPayload snapshot =
                    RequestPassiveWhistleFollowersPayload.snapshot(player, payload.inventorySlot);
            if (snapshot != null) PacketDistributor.sendToPlayer(player, snapshot);
        });
    }

    public static int apply(ServerPlayer player, StopWhistleFieldWorkPayload payload) {
        if (DinoWhistleItem.findInventoryWhistle(player, payload.inventorySlot).isEmpty()) return 0;
        DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.byId(payload.mode);
        int stopped = 0;
        for (FieldDodoEntity dinosaur : DinosaurOwnership.loadedFollowers(player).stream()
                .limit(DinosaurOwnership.followerLimit(player)).toList()) {
            if (!dinosaur.hasFieldWork() || dinosaur.getFieldWorkMode() != mode) continue;
            dinosaur.clearFieldWork();
            stopped++;
        }
        if (stopped > 0) {
            RequestWhistleFollowersPayload.clearStagedCorner(player);
            RequestWhistleFollowersPayload.clearPendingSelection(player);
        }
        return stopped;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
