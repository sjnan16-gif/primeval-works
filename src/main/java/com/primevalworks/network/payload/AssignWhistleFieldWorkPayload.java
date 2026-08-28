package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import com.primevalworks.world.work.DinosaurCommandMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record AssignWhistleFieldWorkPayload(UUID dinosaurId, long selectionToken)
        implements CustomPacketPayload {
    public static final Type<AssignWhistleFieldWorkPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "assign_whistle_field_work"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssignWhistleFieldWorkPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.dinosaurId);
                buffer.writeLong(payload.selectionToken);
            }, buffer -> new AssignWhistleFieldWorkPayload(buffer.readUUID(), buffer.readLong()));

    public static void handle(AssignWhistleFieldWorkPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> apply(player, payload));
    }

    public static boolean apply(ServerPlayer player, AssignWhistleFieldWorkPayload payload) {
        RequestWhistleFollowersPayload.PendingSelection pending =
                RequestWhistleFollowersPayload.pendingSelection(player, payload.selectionToken).orElse(null);
        if (pending == null) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                    "That mark expired. Mark the block again."));
            return false;
        }
        DinoWhistleSettings settings = pending.settings();
        try {
            FieldDodoEntity dinosaur = DinosaurOwnership.findLoaded(player.level().getServer(), payload.dinosaurId);
            boolean availableFollower = DinosaurOwnership.loadedFollowers(player).stream()
                    .limit(DinosaurOwnership.followerLimit(player))
                    .anyMatch(follower -> follower.getUUID().equals(payload.dinosaurId));
            if (!settings.mode().requiresMark()
                    || dinosaur == null || dinosaur.level() != player.level()
                    || !dinosaur.isOwnedBy(player.getUUID())
                    || !availableFollower
                    || dinosaur.getCommandMode() != DinosaurCommandMode.FOLLOW
                    || dinosaur.isOnExpedition() || dinosaur.isIncapacitated()) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        "That companion is no longer available for this order."));
                return false;
            }
            int rating = DinoFieldWorkRules.rating(dinosaur, settings.mode());
            if (!DinoFieldWorkRules.supports(dinosaur.getSpecies(), settings.mode())) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        "That species has a different field specialty."));
                return false;
            }
            if (settings.mode() == DinoWhistleSettings.FieldMode.QUARRY
                    && settings.pattern() == DinoWhistleSettings.Pattern.AREA
                    && !DinoFieldWorkRules.areaWithinLimits(
                            pending.first(), pending.second(), dinosaur.getDinosaurLevel())) {
                int required = DinoFieldWorkRules.requiredLevel(pending.first(), pending.second());
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        required > com.primevalworks.world.entity.DinosaurProgression.MAX_LEVEL
                                ? "That quarry is beyond the maximum field boundary."
                                : "Level this dinosaur to " + required + " to clear that quarry."));
                return false;
            }
            if (rating <= 0
                    || !DinoFieldWorkRules.validTarget(player.level(), pending.first(), settings.mode(), rating)) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        "That companion cannot work this target."));
                return false;
            }
            dinosaur.assignFieldWork(settings, pending.first(), pending.hasSecond() ? pending.second() : null);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                    dinosaur.getDisplayName().getString() + " received the "
                            + DinoFieldWorkRules.specialtyName(settings.mode()) + " order."));
            return dinosaur.hasFieldWork() && dinosaur.getFieldWorkMode() == settings.mode();
        } finally {
            RequestWhistleFollowersPayload.clearStagedCorner(player);
            RequestWhistleFollowersPayload.clearPendingSelection(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
