package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import com.primevalworks.world.work.DinosaurCommandMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record AssignWhistleFieldWorkPayload(UUID dinosaurId, BlockPos first, BlockPos second, boolean hasSecond)
        implements CustomPacketPayload {
    public static final Type<AssignWhistleFieldWorkPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "assign_whistle_field_work"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssignWhistleFieldWorkPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.dinosaurId);
                buffer.writeLong(payload.first.asLong());
                buffer.writeBoolean(payload.hasSecond);
                if (payload.hasSecond) buffer.writeLong(payload.second.asLong());
            }, buffer -> {
                UUID id = buffer.readUUID();
                BlockPos first = BlockPos.of(buffer.readLong());
                boolean hasSecond = buffer.readBoolean();
                return new AssignWhistleFieldWorkPayload(id, first,
                        hasSecond ? BlockPos.of(buffer.readLong()) : first, hasSecond);
            });

    public static void handle(AssignWhistleFieldWorkPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            ItemStack whistle = DinoWhistleItem.findHeld(player);
            DinoWhistleSettings settings = whistle.isEmpty() ? null : DinoWhistleSettings.read(whistle);
            RequestWhistleFollowersPayload selection = new RequestWhistleFollowersPayload(
                    payload.first, payload.second, payload.hasSecond);
            FieldDodoEntity dinosaur = DinosaurOwnership.findLoaded(player.level().getServer(), payload.dinosaurId);
            boolean availableFollower = DinosaurOwnership.loadedFollowers(player).stream()
                    .limit(DinosaurOwnership.followerLimit(player))
                    .anyMatch(follower -> follower.getUUID().equals(payload.dinosaurId));
            if (settings == null || !settings.mode().requiresMark()
                    || dinosaur == null || dinosaur.level() != player.level()
                    || !dinosaur.isOwnedBy(player.getUUID())
                    || !availableFollower
                    || dinosaur.getCommandMode() != DinosaurCommandMode.FOLLOW
                    || dinosaur.isOnExpedition() || dinosaur.isIncapacitated()
                    || !RequestWhistleFollowersPayload.validSelection(player, selection, settings)) return;
            int rating = DinoFieldWorkRules.rating(dinosaur, settings.mode());
            if (!DinoFieldWorkRules.supports(dinosaur.getSpecies(), settings.mode())) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        "That species has a different field specialty."));
                return;
            }
            if (settings.mode() == DinoWhistleSettings.FieldMode.QUARRY
                    && settings.pattern() == DinoWhistleSettings.Pattern.AREA
                    && !DinoFieldWorkRules.areaWithinLimits(
                            payload.first, payload.second, dinosaur.getDinosaurLevel())) {
                int required = DinoFieldWorkRules.requiredLevel(payload.first, payload.second);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        required > com.primevalworks.world.entity.DinosaurProgression.MAX_LEVEL
                                ? "That quarry is beyond the maximum field boundary."
                                : "Level this dinosaur to " + required + " to clear that quarry."));
                return;
            }
            if (rating <= 0
                    || !DinoFieldWorkRules.validTarget(player.level(), payload.first, settings.mode(), rating)) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        "That companion cannot work this target."));
                return;
            }
            dinosaur.assignFieldWork(settings, payload.first, payload.hasSecond ? payload.second : null);
            RequestWhistleFollowersPayload.clearStagedCorner(player);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                    dinosaur.getDisplayName().getString() + " received the "
                            + DinoFieldWorkRules.specialtyName(settings.mode()) + " order."));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
