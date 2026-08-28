package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import com.primevalworks.world.work.DinosaurCommandMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record AssignPassiveWhistleWorkPayload(int inventorySlot, UUID dinosaurId)
        implements CustomPacketPayload {
    public static final Type<AssignPassiveWhistleWorkPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "assign_passive_whistle_work"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssignPassiveWhistleWorkPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeVarInt(payload.inventorySlot);
                buffer.writeUUID(payload.dinosaurId);
            }, buffer -> new AssignPassiveWhistleWorkPayload(buffer.readVarInt(), buffer.readUUID()));

    public static void handle(AssignPassiveWhistleWorkPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            ItemStack whistle = DinoWhistleItem.findInventoryWhistle(player, payload.inventorySlot);
            if (whistle.isEmpty()) return;
            DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);
            if (!settings.mode().isPassive()) return;
            FieldDodoEntity dinosaur = DinosaurOwnership.findLoaded(player.level().getServer(), payload.dinosaurId);
            boolean availableFollower = DinosaurOwnership.loadedFollowers(player).stream()
                    .limit(DinosaurOwnership.followerLimit(player))
                    .anyMatch(follower -> follower.getUUID().equals(payload.dinosaurId));
            if (dinosaur == null || dinosaur.level() != player.level() || !availableFollower
                    || !dinosaur.isOwnedBy(player.getUUID())
                    || dinosaur.getCommandMode() != DinosaurCommandMode.FOLLOW
                    || dinosaur.isOnExpedition() || dinosaur.isIncapacitated()
                    || !DinoFieldWorkRules.supports(dinosaur.getSpecies(), settings.mode())
                    || DinoFieldWorkRules.rating(dinosaur, settings.mode()) <= 0) return;
            boolean assigned = dinosaur.togglePassiveFieldWork(settings);
            String duty = switch (settings.mode()) {
                case QUARRY -> "waiting for a marked quarry.";
                case HARVEST -> "tending nearby crops.";
                case COLLECT -> "retrieving nearby items.";
                case LUMBER -> "waiting for a marked tree.";
            };
            player.sendOverlayMessage(Component.literal(
                    assigned
                            ? dinosaur.getDisplayName().getString() + " is now " + duty
                            : dinosaur.getDisplayName().getString() + " stopped field duty."));
            PassiveWhistleFollowersPayload snapshot =
                    RequestPassiveWhistleFollowersPayload.snapshot(player, payload.inventorySlot);
            if (snapshot != null) PacketDistributor.sendToPlayer(player, snapshot);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
