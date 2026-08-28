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
    private static final String STAGED_CORNER = "PrimevalWhistleCorner";
    private static final String STAGED_DIMENSION = "PrimevalWhistleCornerDimension";
    private static final String STAGED_AT = "PrimevalWhistleCornerAt";
    private static final long STAGED_CORNER_LIFETIME = 20L * 60L;
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
            if (isAreaQuarry(settings) && !payload.hasSecond) {
                if (!stageFirstCorner(player, payload.first, settings)) {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                            "Move closer and mark a safe quarry corner."));
                }
                return;
            }
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
                if (!DinoFieldWorkRules.supports(dinosaur.getSpecies(), settings.mode())) continue;
                int rating = DinoFieldWorkRules.rating(dinosaur, settings.mode());
                boolean valid = rating > 0
                        && DinoFieldWorkRules.validTarget(player.level(), payload.first, settings.mode(), rating)
                        && (settings.mode() != DinoWhistleSettings.FieldMode.QUARRY
                        || settings.pattern() != DinoWhistleSettings.Pattern.AREA
                        || DinoFieldWorkRules.areaWithinLimits(
                                payload.first, payload.second, dinosaur.getDinosaurLevel()));
                entries.add(new WhistleFollowerListPayload.Entry(dinosaur.getId(), dinosaur.getUUID(),
                        dinosaur.getDisplayName().getString(), dinosaur.getSpecies().registryName(),
                        dinosaur.getDinosaurLevel(), rating, valid));
            }
            PacketDistributor.sendToPlayer(player, new WhistleFollowerListPayload(payload.first,
                    payload.second, payload.hasSecond, settings.mode().ordinal(), settings.pattern().ordinal(),
                    settings.range(), entries));
        });
    }

    static boolean validSelection(ServerPlayer player, RequestWhistleFollowersPayload payload,
                                  DinoWhistleSettings settings) {
        double packetReach = 10.0D;
        if (!settings.mode().requiresMark()) return false;
        if (isAreaQuarry(settings)) {
            return payload.hasSecond
                    && player.position().distanceToSqr(payload.second.getCenter()) <= packetReach * packetReach
                    && stagedCornerMatches(player, payload.first)
                    && DinoFieldWorkRules.areaWithinLimits(payload.first, payload.second);
        }
        return !payload.hasSecond
                && player.position().distanceToSqr(payload.first.getCenter()) <= packetReach * packetReach;
    }

    public static void clearStagedCorner(ServerPlayer player) {
        var data = player.getPersistentData();
        data.remove(STAGED_CORNER);
        data.remove(STAGED_DIMENSION);
        data.remove(STAGED_AT);
    }

    private static boolean stageFirstCorner(ServerPlayer player, BlockPos corner,
                                            DinoWhistleSettings settings) {
        double packetReach = 10.0D;
        if (player.position().distanceToSqr(corner.getCenter()) > packetReach * packetReach
                || !DinoFieldWorkRules.validTarget(player.level(), corner, settings.mode(), 4)) return false;
        var data = player.getPersistentData();
        data.putLong(STAGED_CORNER, corner.asLong());
        data.putString(STAGED_DIMENSION, player.level().dimension().identifier().toString());
        data.putLong(STAGED_AT, player.level().getGameTime());
        return true;
    }

    private static boolean stagedCornerMatches(ServerPlayer player, BlockPos corner) {
        var data = player.getPersistentData();
        long age = player.level().getGameTime() - data.getLongOr(STAGED_AT, Long.MIN_VALUE);
        boolean valid = age >= 0L && age <= STAGED_CORNER_LIFETIME
                && data.getLongOr(STAGED_CORNER, Long.MIN_VALUE) == corner.asLong()
                && data.getStringOr(STAGED_DIMENSION, "")
                .equals(player.level().dimension().identifier().toString());
        if (!valid) clearStagedCorner(player);
        return valid;
    }

    private static boolean isAreaQuarry(DinoWhistleSettings settings) {
        return settings.mode() == DinoWhistleSettings.FieldMode.QUARRY
                && settings.pattern() == DinoWhistleSettings.Pattern.AREA;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
