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
                    || DinoFieldWorkRules.rating(dinosaur, settings.mode()) <= 0) return;
            dinosaur.assignPassiveFieldWork(settings);
            player.sendOverlayMessage(Component.literal(dinosaur.getDisplayName().getString() + " is now "
                    + (settings.mode() == DinoWhistleSettings.FieldMode.HARVEST
                    ? "tending nearby crops." : "retrieving nearby items.")));
            PacketDistributor.sendToPlayer(player, new PassiveWhistleFollowersPayload(payload.inventorySlot,
                    DinosaurOwnership.loadedFollowers(player).stream()
                            .limit(DinosaurOwnership.followerLimit(player))
                            .map(follower -> {
                                int rating = DinoFieldWorkRules.rating(follower, settings.mode());
                                return new PassiveWhistleFollowersPayload.Entry(follower.getId(), follower.getUUID(),
                                        follower.getDisplayName().getString(), rating, rating > 0,
                                        follower.hasFieldWork() && follower.getFieldWorkMode() == settings.mode());
                            }).toList()));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
