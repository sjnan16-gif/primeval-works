package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.work.DinoFieldWorkRules;
import com.primevalworks.world.work.DinoWhistleSettings;
import net.minecraft.core.BlockPos;
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

public record RequestWhistleFollowersPayload(BlockPos first, BlockPos second, boolean hasSecond)
        implements CustomPacketPayload {
    public static final Type<RequestWhistleFollowersPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "request_whistle_followers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWhistleFollowersPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.first.asLong());
                buffer.writeBoolean(payload.hasSecond);
                if (payload.hasSecond) buffer.writeLong(payload.second.asLong());
            }, buffer -> {
                BlockPos first = BlockPos.of(buffer.readLong());
                boolean hasSecond = buffer.readBoolean();
                return new RequestWhistleFollowersPayload(first,
                        hasSecond ? BlockPos.of(buffer.readLong()) : first, hasSecond);
            });

    public static void handle(RequestWhistleFollowersPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            ItemStack whistle = DinoWhistleItem.findHeld(player);
            if (whistle.isEmpty()) return;
            DinoWhistleSettings settings = DinoWhistleSettings.read(whistle);
            if (!validSelection(player, payload, settings)) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        settings.pattern() == DinoWhistleSettings.Pattern.AREA
                                ? "Those corners are too far away or the marked area is too large."
                                : "Move closer before marking that block."));
                return;
            }
            List<WhistleFollowerListPayload.Entry> entries = new ArrayList<>();
            for (FieldDodoEntity dinosaur : DinosaurOwnership.loadedFollowers(player)) {
                if (entries.size() >= DinosaurOwnership.followerLimit(player)) break;
                int rating = DinoFieldWorkRules.rating(dinosaur, settings.mode());
                boolean valid = rating > 0 && (settings.mode() == DinoWhistleSettings.FieldMode.COLLECT
                        || DinoFieldWorkRules.validTarget(player.level(), payload.first, settings.mode(), rating)
                        || settings.pattern() == DinoWhistleSettings.Pattern.AREA);
                entries.add(new WhistleFollowerListPayload.Entry(dinosaur.getId(), dinosaur.getUUID(),
                        dinosaur.getDisplayName().getString(), dinosaur.getSpecies().registryName(), rating, valid));
            }
            PacketDistributor.sendToPlayer(player, new WhistleFollowerListPayload(payload.first,
                    payload.second, payload.hasSecond, settings.mode().ordinal(), settings.pattern().ordinal(),
                    settings.continuous(), settings.range(), entries));
        });
    }

    static boolean validSelection(ServerPlayer player, RequestWhistleFollowersPayload payload,
                                  DinoWhistleSettings settings) {
        double packetReach = 10.0D;
        if (player.position().distanceToSqr(payload.first.getCenter()) > packetReach * packetReach
                || payload.hasSecond && player.position().distanceToSqr(payload.second.getCenter()) > packetReach * packetReach) {
            return false;
        }
        if (settings.pattern() == DinoWhistleSettings.Pattern.AREA) {
            return payload.hasSecond && DinoFieldWorkRules.areaWithinLimits(payload.first, payload.second);
        }
        return !payload.hasSecond;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
