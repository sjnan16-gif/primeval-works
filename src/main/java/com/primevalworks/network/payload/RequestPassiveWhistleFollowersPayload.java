package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record RequestPassiveWhistleFollowersPayload(int inventorySlot) implements CustomPacketPayload {
    public static final Type<RequestPassiveWhistleFollowersPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "request_passive_whistle_followers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPassiveWhistleFollowersPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.inventorySlot),
                    buffer -> new RequestPassiveWhistleFollowersPayload(buffer.readVarInt()));

    public static void handle(RequestPassiveWhistleFollowersPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            PassiveWhistleFollowersPayload snapshot = snapshot(player, payload.inventorySlot);
            if (snapshot != null) PacketDistributor.sendToPlayer(player, snapshot);
        });
    }

    public static PassiveWhistleFollowersPayload snapshot(ServerPlayer player, int inventorySlot) {
        ItemStack whistle = DinoWhistleItem.findInventoryWhistle(player, inventorySlot);
        if (whistle.isEmpty()) return null;
        DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);
        List<FieldDodoEntity> followers = DinosaurOwnership.loadedFollowers(player).stream()
                .limit(DinosaurOwnership.followerLimit(player))
                .toList();
        int availableModes = 0;
        for (FieldDodoEntity dinosaur : followers) {
            DinoWhistleSettings.FieldMode specialty = DinoFieldWorkRules.specialty(dinosaur.getSpecies());
            if (specialty != null && DinoFieldWorkRules.rating(dinosaur, specialty) > 0) {
                availableModes |= specialty.availabilityBit();
            }
        }
        List<PassiveWhistleFollowersPayload.Entry> entries = new ArrayList<>();
        int maximumEligibleLevel = 0;
        for (FieldDodoEntity dinosaur : followers) {
            if (!DinoFieldWorkRules.supports(dinosaur.getSpecies(), settings.mode())) continue;
            int rating = DinoFieldWorkRules.rating(dinosaur, settings.mode());
            if (rating <= 0) continue;
            maximumEligibleLevel = Math.max(maximumEligibleLevel, dinosaur.getDinosaurLevel());
            entries.add(new PassiveWhistleFollowersPayload.Entry(dinosaur.getId(), dinosaur.getUUID(),
                    dinosaur.getDisplayName().getString(), rating, true,
                    dinosaur.hasFieldWork() && dinosaur.getFieldWorkMode() == settings.mode()));
        }
        return new PassiveWhistleFollowersPayload(inventorySlot, settings.mode().ordinal(),
                availableModes, maximumEligibleLevel, List.copyOf(entries));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
