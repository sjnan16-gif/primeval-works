package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.CommandTableTestScreen;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record DinosaurRosterPayload(BlockPos tablePos, List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 128;
    public static final Type<DinosaurRosterPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dinosaur_roster")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DinosaurRosterPayload> STREAM_CODEC = StreamCodec.of(
            DinosaurRosterPayload::encode,
            DinosaurRosterPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, DinosaurRosterPayload payload) {
        buffer.writeLong(payload.tablePos.asLong());
        int size = Math.min(MAX_ENTRIES, payload.entries.size());
        buffer.writeVarInt(size);
        for (Entry entry : payload.entries.subList(0, size)) {
            buffer.writeUUID(entry.id());
            buffer.writeUtf(entry.species(), 48);
            buffer.writeUtf(entry.name(), 96);
            buffer.writeVarInt(entry.level());
            buffer.writeVarInt(entry.hunger());
            buffer.writeVarInt(entry.mood());
            buffer.writeFloat(entry.health());
            buffer.writeFloat(entry.maxHealth());
            buffer.writeVarInt(entry.geneticQuality());
            buffer.writeVarInt(entry.mutationMask());
            buffer.writeVarInt(entry.hueVariant() + 8);
            buffer.writeBoolean(entry.originalPigmentRestored());
            buffer.writeBoolean(entry.active());
            buffer.writeVarInt(entry.entityId() + 1);
            buffer.writeVarLong(entry.recoveryTicksRemaining());
            buffer.writeBoolean(entry.onExpedition());
            buffer.writeVarInt(entry.expeditionTier());
            buffer.writeVarLong(entry.expeditionTicksRemaining());
        }
    }

    private static DinosaurRosterPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos tablePos = BlockPos.of(buffer.readLong());
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) throw new IllegalArgumentException("Invalid dinosaur roster size: " + size);
        List<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new Entry(
                    buffer.readUUID(), buffer.readUtf(48), buffer.readUtf(96), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt() - 8,
                    buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readVarInt() - 1, Math.max(0L, buffer.readVarLong()),
                    buffer.readBoolean(), buffer.readVarInt(), Math.max(0L, buffer.readVarLong())
            ));
        }
        return new DinosaurRosterPayload(tablePos, List.copyOf(entries));
    }

    public static void send(ServerPlayer player, BlockPos tablePos) {
        DinosaurOwnership.activateForTable(player, tablePos, false);
        List<DinosaurOwnership.OwnedDinosaur> records = DinosaurOwnership.records(player);
        List<UUID> activeIds = DinosaurOwnership.activeIds(player);
        Set<UUID> activeSet = new HashSet<>(activeIds);
        Map<UUID, DinosaurOwnership.OwnedDinosaur> byId = new HashMap<>();
        records.forEach(record -> byId.put(record.id(), record));
        List<Entry> result = new ArrayList<>(records.size());
        for (UUID id : activeIds) {
            DinosaurOwnership.OwnedDinosaur record = byId.get(id);
            if (record != null) result.add(from(player, record, true));
        }
        for (DinosaurOwnership.OwnedDinosaur record : records) {
            if (!activeSet.contains(record.id())) result.add(from(player, record, false));
        }
        PacketDistributor.sendToPlayer(player, new DinosaurRosterPayload(tablePos, List.copyOf(result)));
    }

    private static Entry from(ServerPlayer player, DinosaurOwnership.OwnedDinosaur record, boolean active) {
        FieldDodoEntity entity = DinosaurOwnership.findLoaded(player.level().getServer(), record.id());
        long now = player.level().getGameTime();
        boolean onExpedition = entity != null
                ? entity.isOnExpedition() && entity.getExpeditionTicksRemaining() > 0L
                : record.isOnExpedition(now);
        boolean originalPigmentRestored = entity != null
                ? entity.hasRestoredOriginalPigment()
                : record.snapshot().getBooleanOr("PrimevalOriginalPigmentRestored", false);
        String name = entity != null ? entity.getDisplayName().getString() : record.name();
        int level = entity != null ? entity.getDinosaurLevel() : record.level();
        int hunger = entity != null ? entity.getHunger() : record.hunger();
        int mood = entity != null ? entity.getMood() : record.mood();
        float health = entity != null ? entity.getHealth() : record.health();
        float maxHealth = entity != null ? entity.getMaxHealth() : record.maxHealth();
        int quality = entity != null ? entity.getGeneticQuality() : record.geneticQuality();
        int mutations = entity != null ? entity.getMutationMask() : record.mutationMask();
        int hue = entity != null ? entity.getHueVariant() : record.hueVariant();
        return new Entry(
                record.id(), record.species().registryName(), name, level, hunger, mood,
                health, maxHealth, quality, mutations, hue,
                originalPigmentRestored,
                active, entity == null ? -1 : entity.getId(),
                Math.max(0L, record.recoveryUntilTick() - now),
                onExpedition,
                entity != null ? entity.getExpeditionTier() : record.expeditionTier(),
                entity != null ? entity.getExpeditionTicksRemaining() : record.expeditionTicksRemaining(now)
        );
    }

    public static void handle(DinosaurRosterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CommandTableTestScreen.acceptDinosaurRoster(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(UUID id, String species, String name, int level, int hunger, int mood,
                         float health, float maxHealth, int geneticQuality, int mutationMask,
                         int hueVariant, boolean originalPigmentRestored, boolean active, int entityId, long recoveryTicksRemaining,
                         boolean onExpedition, int expeditionTier, long expeditionTicksRemaining) {
    }
}
