package com.primevalworks.world.ownership;

import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.registry.ModEntities;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.block.TurbinePartBlock;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.work.BaseInventoryIndex;
import com.primevalworks.world.work.DinosaurCommandMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DinosaurOwnership {
    public static final int ACTIVE_LIMIT = 14;
    public static final int STARTING_ACTIVE_LIMIT = 7;
    public static final int DEPOT_PAGE_SIZE = 16;
    private static final int MAX_OWNED = 128;
    private static final String OWNED_COUNT = "PrimevalOwnedDinosaurCount";
    private static final String OWNED_PREFIX = "PrimevalOwnedDinosaur";
    private static final String OWNERSHIP_SCHEMA = "PrimevalOwnershipSchema";
    private static final int CURRENT_SCHEMA = 3;
    private static final String ACTIVE_COUNT = "PrimevalActiveDinosaurCount";
    private static final String ACTIVE_PREFIX = "PrimevalActiveDinosaur";
    private static final String SNAPSHOT_DIMENSION = "PrimevalSnapshotDimension";
    private static final String SNAPSHOT_POSITION = "PrimevalSnapshotPosition";

    private DinosaurOwnership() {
    }

    public static void register(ServerPlayer player, FieldDodoEntity dinosaur) {
        dinosaur.setDinosaurOwner(player.getUUID());
        List<OwnedDinosaur> records = new ArrayList<>(records(player));
        upsert(records, capture(dinosaur));
        writeRecords(player.getPersistentData(), records);
    }

    /** Keeps the portable depot snapshot aligned with a live, server-owned dinosaur. */
    public static void syncRecord(FieldDodoEntity dinosaur) {
        if (!(dinosaur.level() instanceof ServerLevel level) || dinosaur.isRemoved()) return;
        UUID ownerId = dinosaur.getDinosaurOwner().orElse(null);
        if (ownerId == null) return;
        ServerPlayer owner = findOnlinePlayer(level.getServer(), ownerId);
        if (owner == null) return;
        List<OwnedDinosaur> records = new ArrayList<>(records(owner));
        upsert(records, capture(dinosaur));
        writeRecords(owner.getPersistentData(), records);
    }

    /** Flushes every loaded companion before the player's persistent data is saved. */
    public static void syncLoaded(ServerPlayer player) {
        refresh(player);
    }

    public static void remove(ServerPlayer player, UUID dinosaurId) {
        List<OwnedDinosaur> records = new ArrayList<>(records(player));
        records.removeIf(record -> record.id().equals(dinosaurId));
        writeRecords(player.getPersistentData(), records);
        List<UUID> active = new ArrayList<>(activeIds(player));
        active.remove(dinosaurId);
        writeActive(player.getPersistentData(), active);
    }

    public static void permanentlyRemove(ServerPlayer owner, FieldDodoEntity dinosaur) {
        if (!dinosaur.isOwnedBy(owner.getUUID())) return;
        returnCarriedCargo(dinosaur);
        remove(owner, dinosaur.getUUID());
        dinosaur.unlinkFromCommandTable();
    }

    public static boolean returnToReserveAfterDefeat(FieldDodoEntity dinosaur) {
        if (!(dinosaur.level() instanceof ServerLevel level)) return false;
        UUID ownerId = dinosaur.getDinosaurOwner().orElse(null);
        if (ownerId == null) return false;
        ServerPlayer owner = findOnlinePlayer(level.getServer(), ownerId);
        if (owner == null) return false;

        dinosaur.prepareForRecoverySnapshot();
        dinosaur.setHealth(Math.max(1.0F, dinosaur.getMaxHealth() * 0.20F));
        List<OwnedDinosaur> records = new ArrayList<>(records(owner));
        long recoveryUntil = level.getGameTime() + recoveryDurationTicks(dinosaur.getSpecies());
        returnCarriedCargo(dinosaur);
        upsert(records, capture(dinosaur).withRecoveryUntil(recoveryUntil));
        writeRecords(owner.getPersistentData(), records);
        List<UUID> active = new ArrayList<>(activeIds(owner));
        active.remove(dinosaur.getUUID());
        writeActive(owner.getPersistentData(), active);
        dinosaur.unlinkFromCommandTable();
        dinosaur.discard();
        owner.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                dinosaur.getDisplayName().getString() + " was injured. They are recovering in the depot."
        ));
        return true;
    }

    public static void copy(ServerPlayer original, ServerPlayer replacement) {
        writeRecords(replacement.getPersistentData(), records(original));
        writeActive(replacement.getPersistentData(), activeIds(original));
    }

    public static List<OwnedDinosaur> refresh(ServerPlayer player) {
        List<OwnedDinosaur> records = new ArrayList<>(records(player));
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            for (OwnedDinosaur record : List.copyOf(records)) {
                Entity found = level.getEntity(record.id());
                if (found instanceof FieldDodoEntity dinosaur && dinosaur.isAlive() && dinosaur.isOwnedBy(player.getUUID())) {
                    dinosaur.reconcilePersistentTimedState();
                    upsert(records, capture(dinosaur));
                }
            }
        }
        writeRecords(player.getPersistentData(), records);
        return List.copyOf(records);
    }

    public static void adoptLinkedDinosaurs(ServerPlayer player, BlockPos tablePos) {
        adoptLinkedDinosaurs(player, player.level(), tablePos);
    }

    private static void adoptLinkedDinosaurs(ServerPlayer player, ServerLevel tableLevel, BlockPos tablePos) {
        int radius = 64;
        List<FieldDodoEntity> nearby = tableLevel.getEntitiesOfClass(
                FieldDodoEntity.class,
                new net.minecraft.world.phys.AABB(tablePos).inflate(radius),
                dinosaur -> dinosaur.isAlive()
                        && dinosaur.getCommandTablePos().filter(tablePos::equals).isPresent()
                        && (dinosaur.getDinosaurOwner().isEmpty() || dinosaur.isOwnedBy(player.getUUID()))
        );
        for (FieldDodoEntity dinosaur : nearby) register(player, dinosaur);
    }

    public static void activateForTable(ServerPlayer player, BlockPos tablePos, boolean spawnMissing) {
        activateForTable(player, player.level(), tablePos, spawnMissing, spawnMissing);
    }

    /** Restores the saved active crew after login without filling empty slots from the depot. */
    public static void restoreActiveForTable(ServerPlayer player, BlockPos tablePos) {
        activateForTable(player, player.level(), tablePos, false, true);
    }

    public static void prepareActiveRestore(ServerPlayer player) {
        Set<UUID> active = Set.copyOf(activeIds(player));
        for (OwnedDinosaur record : records(player)) {
            if (active.contains(record.id())) findOrLoad(player.level().getServer(), record);
        }
    }

    public static void restoreActiveForTable(ServerPlayer player, CommandTableBlock.ClaimedTable table) {
        activateForTable(player, table.level(), table.pos(), false, true);
    }

    private static void activateForTable(ServerPlayer player, ServerLevel tableLevel, BlockPos tablePos,
                                         boolean fillEmptySlots, boolean restoreMissingActive) {
        adoptLinkedDinosaurs(player, tableLevel, tablePos);
        List<OwnedDinosaur> records = new ArrayList<>(refresh(player));
        Set<UUID> ownedIds = new HashSet<>();
        records.forEach(record -> ownedIds.add(record.id()));
        List<UUID> active = new ArrayList<>(activeIds(player));
        int activeLimit = activeLimit(player, tableLevel, tablePos);
        long now = tableLevel.getGameTime();
        active.removeIf(id -> !ownedIds.contains(id) || find(records, id)
                .map(record -> record.recoveryUntilTick() > now).orElse(true));
        while (active.size() > activeLimit) {
            UUID storedId = active.removeLast();
            OwnedDinosaur storedRecord = find(records, storedId).orElse(null);
            FieldDodoEntity stored = storedRecord == null ? null : findOrLoad(player.level().getServer(), storedRecord);
            if (stored != null) {
                returnCarriedCargo(stored);
                stored.setCommandMode(DinosaurCommandMode.HOME);
                upsert(records, capture(stored));
                stored.unlinkFromCommandTable();
                stored.discard();
            }
        }
        if (fillEmptySlots) {
            for (OwnedDinosaur record : records) {
                if (active.size() >= activeLimit) break;
                if (record.recoveryUntilTick() <= now && !active.contains(record.id())) active.add(record.id());
            }
        }
        writeActive(player.getPersistentData(), active);

        int restoredFollowers = 0;
        int followerLimit = tableLevel.getBlockEntity(tablePos) instanceof CommandTableBlockEntity table
                && table.isOwnedBy(player.getUUID()) ? table.followerCapacity() : 1;
        for (int index = 0; index < active.size(); index++) {
            OwnedDinosaur record = find(records, active.get(index)).orElse(null);
            if (record == null) continue;
            FieldDodoEntity dinosaur = restoreMissingActive
                    ? findOrLoad(player.level().getServer(), record)
                    : findLoaded(player.level().getServer(), record.id());
            if (dinosaur == null && restoreMissingActive) {
                dinosaur = spawn(tableLevel, record, slotPosition(tablePos, index));
            }
            if (dinosaur == null) continue;
            dinosaur.setDinosaurOwner(player.getUUID());
            if (dinosaur.getCommandMode() == DinosaurCommandMode.FOLLOW) {
                if (restoredFollowers >= followerLimit) dinosaur.setCommandMode(DinosaurCommandMode.HOME);
                else restoredFollowers++;
            }
            boolean changingBase = dinosaur.getCommandTablePos().filter(tablePos::equals).isEmpty();
            dinosaur.linkToCommandTable(tablePos);
            if (changingBase) moveToSlot(dinosaur, tablePos, index);
            upsert(records, capture(dinosaur));
        }

        Set<UUID> activeSet = Set.copyOf(active);
        for (OwnedDinosaur record : records) {
            if (activeSet.contains(record.id())) continue;
            FieldDodoEntity dinosaur = findLoaded(player.level().getServer(), record.id());
            if (dinosaur != null) {
                returnCarriedCargo(dinosaur);
                dinosaur.setCommandMode(DinosaurCommandMode.HOME);
                upsert(records, capture(dinosaur));
                dinosaur.discard();
            }
        }
        writeRecords(player.getPersistentData(), records);
    }

    public static boolean addToActiveIfRoom(ServerPlayer player, FieldDodoEntity dinosaur, BlockPos tablePos) {
        register(player, dinosaur);
        if (records(player).stream().noneMatch(record -> record.id().equals(dinosaur.getUUID()))) return false;
        List<UUID> active = new ArrayList<>(activeIds(player));
        if (active.contains(dinosaur.getUUID())) {
            dinosaur.linkToCommandTable(tablePos);
            syncRecord(dinosaur);
            return true;
        }
        if (active.size() >= activeLimit(player, tablePos)) return false;
        active.add(dinosaur.getUUID());
        writeActive(player.getPersistentData(), active);
        dinosaur.linkToCommandTable(tablePos);
        syncRecord(dinosaur);
        return true;
    }

    public static SwapResult swapIntoActive(ServerPlayer player, BlockPos tablePos, UUID incomingId, int targetSlot) {
        List<OwnedDinosaur> records = new ArrayList<>(refresh(player));
        OwnedDinosaur incoming = find(records, incomingId).orElse(null);
        if (incoming == null) return new SwapResult(false, "That dinosaur is no longer in your depot.");
        if (incoming.recoveryUntilTick() > player.level().getGameTime()) {
            return new SwapResult(false, incoming.name() + " is still recovering.");
        }
        if (incoming.isOnExpedition(player.level().getGameTime())) {
            return new SwapResult(false, incoming.name() + " is away on an expedition.");
        }
        List<UUID> active = new ArrayList<>(activeIds(player));
        int activeLimit = activeLimit(player, tablePos);
        if (targetSlot < 0 || targetSlot >= activeLimit) {
            return new SwapResult(false, "Drop the dinosaur onto an unlocked base slot.");
        }
        targetSlot = Math.min(targetSlot, active.size());
        UUID targetId = targetSlot < active.size() ? active.get(targetSlot) : null;
        if (targetId != null && !targetId.equals(incomingId)) {
            OwnedDinosaur targetRecord = find(records, targetId).orElse(null);
            FieldDodoEntity targetEntity = findLoaded(player.level().getServer(), targetId);
            boolean targetAway = targetEntity != null
                    ? targetEntity.isOnExpedition()
                    : targetRecord != null && targetRecord.isOnExpedition(player.level().getGameTime());
            if (targetAway) {
                String targetName = targetRecord == null ? "That dinosaur" : targetRecord.name();
                return new SwapResult(false,
                        targetName + " is away. Its crew slot stays reserved until it returns.");
            }
        }

        int existingIncoming = active.indexOf(incomingId);
        if (existingIncoming >= 0) {
            active.remove(existingIncoming);
            targetSlot = Math.min(targetSlot, active.size());
            active.add(targetSlot, incomingId);
            writeActive(player.getPersistentData(), active);
            repositionLoadedCrew(player, tablePos, active);
            return new SwapResult(true, incoming.name() + " moved to base slot " + (targetSlot + 1) + ".");
        }
        UUID outgoingId = targetSlot < active.size() ? active.get(targetSlot) : null;
        if (outgoingId != null) {
            OwnedDinosaur outgoingRecord = find(records, outgoingId).orElse(null);
            if (outgoingRecord != null && outgoingRecord.isOnExpedition(player.level().getGameTime())) {
                return new SwapResult(false, outgoingRecord.name() + " cannot leave the crew until the expedition returns.");
            }
            FieldDodoEntity outgoing = outgoingRecord == null
                    ? null
                    : findOrLoad(player.level().getServer(), outgoingRecord);
            if (outgoing != null) {
                returnCarriedCargo(outgoing);
                outgoing.setCommandMode(DinosaurCommandMode.HOME);
                upsert(records, capture(outgoing));
                outgoing.discard();
            }
            active.set(targetSlot, incomingId);
        } else {
            active.add(incomingId);
        }

        FieldDodoEntity incomingEntity = findOrLoad(player.level().getServer(), incoming);
        if (incomingEntity == null) incomingEntity = spawn(player.level(), incoming, slotPosition(tablePos, targetSlot));
        if (incomingEntity == null) return new SwapResult(false, "There is no safe room beside the Command Table.");
        incomingEntity.setDinosaurOwner(player.getUUID());
        if (incomingEntity.getCommandMode() == DinosaurCommandMode.FOLLOW
                && followerCount(player) >= followerLimit(player)) {
            incomingEntity.setCommandMode(DinosaurCommandMode.HOME);
        }
        incomingEntity.linkToCommandTable(tablePos);
        moveToSlot(incomingEntity, tablePos, targetSlot);
        writeRecords(player.getPersistentData(), records);
        writeActive(player.getPersistentData(), active);
        repositionLoadedCrew(player, tablePos, active);
        return new SwapResult(true, incoming.name() + " joined the active base crew.");
    }

    public static int storeAllActive(ServerPlayer player) {
        List<UUID> active = new ArrayList<>(activeIds(player));
        if (active.isEmpty()) return 0;

        List<OwnedDinosaur> records = new ArrayList<>(refresh(player));
        long now = player.level().getGameTime();
        List<UUID> storing = active.stream()
                .filter(id -> find(records, id).map(record -> !record.isOnExpedition(now)).orElse(false))
                .toList();
        List<UUID> remaining = active.stream().filter(id -> !storing.contains(id)).toList();

        // The roster is the authority. Commit it first so a companion that reloads while
        // this transaction is finishing cannot resume work as an active world entity.
        writeActive(player.getPersistentData(), remaining);
        int storedCount = 0;
        for (UUID dinosaurId : storing) {
            storeWorldAuthority(player, records, dinosaurId);
            storedCount++;
        }
        writeRecords(player.getPersistentData(), records);
        return storedCount;
    }

    public static SwapResult storeActive(ServerPlayer player, UUID dinosaurId) {
        List<UUID> active = new ArrayList<>(activeIds(player));
        if (!active.contains(dinosaurId)) {
            return new SwapResult(false, "That dinosaur is not part of the active crew.");
        }
        List<OwnedDinosaur> records = new ArrayList<>(refresh(player));
        OwnedDinosaur record = find(records, dinosaurId).orElse(null);
        if (record != null && record.isOnExpedition(player.level().getGameTime())) {
            return new SwapResult(false, record.name() + " cannot enter the depot until the expedition returns.");
        }
        active.remove(dinosaurId);
        writeActive(player.getPersistentData(), active);
        String name = storeWorldAuthority(player, records, dinosaurId);
        writeRecords(player.getPersistentData(), records);
        CommandTableBlock.getClaimedTable(player).ifPresent(table -> repositionLoadedCrew(player, table.pos(), active));
        return new SwapResult(true, name + " returned to the depot.");
    }

    private static String storeWorldAuthority(ServerPlayer player, List<OwnedDinosaur> records, UUID dinosaurId) {
        OwnedDinosaur record = find(records, dinosaurId).orElse(null);
        String name = record == null ? "Dinosaur" : record.name();
        List<FieldDodoEntity> copies = findLoadedCopies(player.level().getServer(), dinosaurId).stream()
                .filter(dinosaur -> dinosaur.isOwnedBy(player.getUUID()))
                .toList();
        FieldDodoEntity authority = copies.isEmpty() ? null : copies.getFirst();
        if (authority != null) {
            returnCarriedCargo(authority);
            authority.setCommandMode(DinosaurCommandMode.HOME);
            upsert(records, normalizeDepotSnapshot(capture(authority)));
            name = authority.getDisplayName().getString();
        } else if (record != null) {
            upsert(records, normalizeDepotSnapshot(record));
        }
        for (FieldDodoEntity copy : copies) {
            if (copy != authority) copy.takeCarriedStackForStorage();
            copy.unlinkFromCommandTable();
            copy.discard();
        }
        return name;
    }

    private static OwnedDinosaur normalizeDepotSnapshot(OwnedDinosaur record) {
        CompoundTag snapshot = record.snapshot();
        snapshot.putInt("PrimevalCommandMode", DinosaurCommandMode.HOME.ordinal());
        snapshot.remove("PrimevalStayPosition");
        snapshot.putInt("PrimevalWorkerCooldown", 0);
        snapshot.putInt("PrimevalWorkAction", 0);
        snapshot.putInt("PrimevalWorkActionProgress", 0);
        snapshot.putInt("PrimevalWorkActionDuration", 0);
        snapshot.remove("PrimevalWorkActionPos");
        return new OwnedDinosaur(
                record.id(), record.species(), record.name(), record.level(), record.hunger(), record.mood(),
                record.health(), record.maxHealth(), record.geneticQuality(), record.mutationMask(),
                record.hueVariant(), record.recoveryUntilTick(), snapshot
        );
    }

    public static int recallActive(ServerPlayer player, BlockPos tablePos) {
        List<UUID> active = new ArrayList<>(activeIds(player));
        int recalled = 0;
        for (UUID dinosaurId : active) {
            if (recallActive(player, tablePos, dinosaurId).success()) recalled++;
        }
        return recalled;
    }

    public static SwapResult recallActive(ServerPlayer player, BlockPos tablePos, UUID dinosaurId) {
        List<UUID> active = new ArrayList<>(activeIds(player));
        int slot = active.indexOf(dinosaurId);
        if (slot < 0) return new SwapResult(false, "That dinosaur is not part of the active crew.");

        List<OwnedDinosaur> records = new ArrayList<>(refresh(player));
        OwnedDinosaur record = find(records, dinosaurId).orElse(null);
        if (record == null) return new SwapResult(false, "That companion is no longer available.");
        long now = player.level().getGameTime();
        if (record.recoveryUntilTick() > now) {
            return new SwapResult(false, record.name() + " is still recovering.");
        }
        if (record.isOnExpedition(now)) {
            return new SwapResult(false, record.name() + " cannot return before the expedition ends.");
        }

        FieldDodoEntity dinosaur = findOrLoad(player.level().getServer(), record);
        if (dinosaur != null && dinosaur.level() != player.level()) {
            upsert(records, capture(dinosaur));
            record = find(records, dinosaurId).orElse(record);
            dinosaur.discard();
            dinosaur = null;
        }
        if (dinosaur == null) dinosaur = spawn(player.level(), record, slotPosition(tablePos, slot));
        if (dinosaur == null) return new SwapResult(false, "There is no safe room beside the Command Table.");
        if (dinosaur.isOnExpedition()) {
            return new SwapResult(false, dinosaur.getDisplayName().getString()
                    + " cannot return before the expedition ends.");
        }

        dinosaur.setDinosaurOwner(player.getUUID());
        dinosaur.linkToCommandTable(tablePos);
        dinosaur.getNavigation().stop();
        moveToSlot(dinosaur, tablePos, slot);
        upsert(records, capture(dinosaur));
        writeRecords(player.getPersistentData(), records);
        return new SwapResult(true, dinosaur.getDisplayName().getString() + " returned to the Command Table.");
    }

    public static int activeLimit(ServerPlayer player, BlockPos tablePos) {
        return activeLimit(player, player.level(), tablePos);
    }

    public static int followerLimit(ServerPlayer player) {
        CommandTableBlock.ClaimedTable claimed = CommandTableBlock.getClaimedTable(player).orElse(null);
        if (claimed == null) return 1;
        claimed.level().getChunkAt(claimed.pos());
        if (claimed.level().getBlockEntity(claimed.pos()) instanceof CommandTableBlockEntity table
                && table.isOwnedBy(player.getUUID())) {
            return table.followerCapacity();
        }
        return 1;
    }

    public static int followerCount(ServerPlayer player) {
        Set<UUID> active = Set.copyOf(activeIds(player));
        int followers = 0;
        for (OwnedDinosaur record : records(player)) {
            if (!active.contains(record.id()) || record.recoveryUntilTick() > player.level().getGameTime()) continue;
            FieldDodoEntity loaded = findLoaded(player.level().getServer(), record.id());
            DinosaurCommandMode mode = loaded != null
                    ? loaded.getCommandMode()
                    : DinosaurCommandMode.byId(record.snapshot().getIntOr("PrimevalCommandMode", 0));
            if (mode == DinosaurCommandMode.FOLLOW) followers++;
        }
        return followers;
    }

    public static List<FieldDodoEntity> loadedFollowers(ServerPlayer player) {
        Set<UUID> active = Set.copyOf(activeIds(player));
        List<FieldDodoEntity> followers = new ArrayList<>();
        for (UUID id : active) {
            FieldDodoEntity dinosaur = findLoaded(player.level().getServer(), id);
            if (dinosaur != null && dinosaur.isOwnedBy(player.getUUID())
                    && dinosaur.level() == player.level()
                    && dinosaur.getCommandMode() == DinosaurCommandMode.FOLLOW
                    && !dinosaur.isOnExpedition() && !dinosaur.isIncapacitated()) {
                followers.add(dinosaur);
            }
        }
        return List.copyOf(followers);
    }

    public static SwapResult setCommandMode(ServerPlayer player, FieldDodoEntity dinosaur,
                                            DinosaurCommandMode mode) {
        if (!dinosaur.isAlive() || !dinosaur.isOwnedBy(player.getUUID())) {
            return new SwapResult(false, "That companion is no longer available.");
        }
        if (!activeIds(player).contains(dinosaur.getUUID())) {
            return new SwapResult(false, "Only active base companions can follow commands.");
        }
        if (dinosaur.isOnExpedition() || dinosaur.isIncapacitated()) {
            return new SwapResult(false, "That companion cannot change commands right now.");
        }
        if (mode == DinosaurCommandMode.FOLLOW
                && dinosaur.getCommandMode() != DinosaurCommandMode.FOLLOW
                && followerCount(player) >= followerLimit(player)) {
            return new SwapResult(false, "Follower slots are full. Upgrade Field Command at the Command Table.");
        }
        dinosaur.setCommandMode(mode);
        return new SwapResult(true, switch (mode) {
            case HOME -> dinosaur.getDisplayName().getString() + " is returning to base duty.";
            case STAY -> dinosaur.getDisplayName().getString() + " will hold this position.";
            case FOLLOW -> dinosaur.getDisplayName().getString() + " is following you.";
        });
    }

    private static int activeLimit(ServerPlayer player, ServerLevel tableLevel, BlockPos tablePos) {
        if (tableLevel.getBlockEntity(tablePos) instanceof CommandTableBlockEntity table
                && table.isOwnedBy(player.getUUID())) {
            return Mth.clamp(table.activeDinosaurCapacity(), STARTING_ACTIVE_LIMIT, ACTIVE_LIMIT);
        }
        return STARTING_ACTIVE_LIMIT;
    }

    public static List<OwnedDinosaur> records(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int count = Mth.clamp(data.getIntOr(OWNED_COUNT, 0), 0, MAX_OWNED);
        List<OwnedDinosaur> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String prefix = OWNED_PREFIX + index;
            try {
                UUID id = UUID.fromString(data.getStringOr(prefix + "Id", ""));
                DinosaurSpecies species = DinosaurSpecies.byRegistryName(data.getStringOr(prefix + "Species", "field_dodo"));
                String name = data.getStringOr(prefix + "Name", species.registryName().replace('_', ' '));
                int level = Mth.clamp(data.getIntOr(prefix + "Level", 1), 1, 100);
                int quality = Mth.clamp(data.getIntOr(prefix + "Quality", 50), 0, 100);
                int mutations = data.getIntOr(OWNERSHIP_SCHEMA, 0) >= CURRENT_SCHEMA
                        ? data.getIntOr(prefix + "Mutations", 0)
                                & (FieldDodoEntity.MUTATION_HUGE | FieldDodoEntity.MUTATION_ALBINO)
                        : 0;
                float storedHealth = Math.max(1.0F, data.getIntOr(prefix + "Health", 100) / 100.0F);
                float storedMaximum = Math.max(1.0F,
                        data.getIntOr(prefix + "MaxHealth", (int)species.baseHealth() * 100) / 100.0F);
                float expectedMaximum = FieldDodoEntity.expectedMaxHealth(species, quality, mutations, level);
                float health = storedHealth;
                float maximum = storedMaximum;
                if (Math.abs(storedMaximum - expectedMaximum) > 0.05F) {
                    health = expectedMaximum * Mth.clamp(storedHealth / storedMaximum, 0.0F, 1.0F);
                    maximum = expectedMaximum;
                }
                result.add(new OwnedDinosaur(
                        id, species, name,
                        level,
                        Mth.clamp(data.getIntOr(prefix + "Hunger", 64), 0, 100),
                        Mth.clamp(data.getIntOr(prefix + "Mood", 68), 0, 100),
                        health,
                        maximum,
                        quality,
                        mutations,
                        Mth.clamp(data.getIntOr(prefix + "Hue", 0), -8, 8),
                        Math.max(0L, data.getLongOr(prefix + "RecoveryUntil", 0L)),
                        data.getCompoundOrEmpty(prefix + "State")
                ));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return List.copyOf(result);
    }

    public static List<UUID> activeIds(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int count = Mth.clamp(data.getIntOr(ACTIVE_COUNT, 0), 0, ACTIVE_LIMIT);
        List<UUID> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            try {
                UUID id = UUID.fromString(data.getStringOr(ACTIVE_PREFIX + index, ""));
                if (!result.contains(id)) result.add(id);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return List.copyOf(result);
    }

    public static @Nullable FieldDodoEntity findLoaded(MinecraftServer server, UUID dinosaurId) {
        List<FieldDodoEntity> copies = findLoadedCopies(server, dinosaurId);
        return copies.isEmpty() ? null : copies.getFirst();
    }

    private static List<FieldDodoEntity> findLoadedCopies(MinecraftServer server, UUID dinosaurId) {
        List<FieldDodoEntity> copies = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(dinosaurId);
            if (entity instanceof FieldDodoEntity dinosaur && dinosaur.isAlive()) {
                dinosaur.reconcilePersistentTimedState();
                if (!dinosaur.isRemoved()) copies.add(dinosaur);
            }
        }
        return List.copyOf(copies);
    }

    public static boolean hasActiveWorldAuthority(FieldDodoEntity dinosaur) {
        if (!(dinosaur.level() instanceof ServerLevel level)) return true;
        UUID ownerId = dinosaur.getDinosaurOwner().orElse(null);
        if (ownerId == null) return true;
        ServerPlayer owner = findOnlinePlayer(level.getServer(), ownerId);
        if (owner == null || activeIds(owner).contains(dinosaur.getUUID())) return true;
        // Before the first table, a hatchling may wait physically beside its owner. Once a
        // table exists, every non-active record is depot/expedition/recovery state and must
        // never retain a second world authority when its old chunk loads later.
        return CommandTableBlock.getClaimedTable(owner).isEmpty();
    }

    public static @Nullable ServerPlayer findOnlinePlayer(MinecraftServer server, UUID playerId) {
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (player.getUUID().equals(playerId)) return player;
            }
        }
        return server.getPlayerList().getPlayer(playerId);
    }

    private static @Nullable FieldDodoEntity findOrLoad(MinecraftServer server, OwnedDinosaur record) {
        FieldDodoEntity loaded = findLoaded(server, record.id());
        if (loaded != null) return loaded;
        CompoundTag snapshot = record.snapshot();
        if (!snapshot.contains(SNAPSHOT_DIMENSION) || !snapshot.contains(SNAPSHOT_POSITION)) return null;
        Identifier dimensionId = Identifier.tryParse(snapshot.getStringOr(SNAPSHOT_DIMENSION, ""));
        if (dimensionId == null) return null;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (level == null) return null;
        BlockPos lastPosition = BlockPos.of(snapshot.getLongOr(SNAPSHOT_POSITION, 0L));
        level.getChunkAt(lastPosition);
        Entity entityAtLastPosition = level.getEntity(record.id());
        if (entityAtLastPosition instanceof FieldDodoEntity dinosaur && dinosaur.isAlive()) {
            dinosaur.reconcilePersistentTimedState();
            return dinosaur;
        }
        // A companion may cross a chunk border after its last portable snapshot. Loading only
        // the old chunk made recall miss the real entity and could create a duplicate from the
        // stale depot copy. Owned workers stay inside the linked base, so load that bounded area.
        BlockPos searchCenter = snapshot.contains("PrimevalCommandTable")
                ? BlockPos.of(snapshot.getLongOr("PrimevalCommandTable", lastPosition.asLong()))
                : lastPosition;
        level.getChunkAt(searchCenter);
        int radius = snapshot.contains("PrimevalCommandTable")
                ? Math.max(16, CommandTableBlock.baseRadius(level, searchCenter) + 8)
                : 24;
        int minimumChunkX = (searchCenter.getX() - radius) >> 4;
        int maximumChunkX = (searchCenter.getX() + radius) >> 4;
        int minimumChunkZ = (searchCenter.getZ() - radius) >> 4;
        int maximumChunkZ = (searchCenter.getZ() + radius) >> 4;
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
        Entity entity = level.getEntity(record.id());
        if (entity instanceof FieldDodoEntity dinosaur && dinosaur.isAlive()) {
            dinosaur.reconcilePersistentTimedState();
            return dinosaur;
        }
        return null;
    }

    public static boolean hasEnergyStationAssignment(ServerPlayer player, BlockPos station, UUID exceptId) {
        CommandTableBlock.ClaimedTable table = CommandTableBlock.getClaimedTable(player).orElse(null);
        if (table != null) {
            double radius = CommandTableBlock.baseRadius(table.level(), table.pos()) + 4.0D;
            List<FieldDodoEntity> loadedWorkers = table.level().getEntitiesOfClass(
                    FieldDodoEntity.class,
                    new net.minecraft.world.phys.AABB(table.pos()).inflate(radius, 40.0D, radius),
                    dinosaur -> !dinosaur.getUUID().equals(exceptId)
                            && dinosaur.isAlive()
                            && dinosaur.isOwnedBy(player.getUUID())
                            && dinosaur.isWorkEnabled()
                            && dinosaur.getWorkJobIndex() == 2
            );
            for (FieldDodoEntity worker : loadedWorkers) {
                if (worker.getWorkWorkstationPositions().stream()
                        .map(pos -> canonicalTurbinePosition(table.level(), pos))
                        .anyMatch(station::equals)) return true;
            }
        }
        Set<UUID> active = Set.copyOf(activeIds(player));
        for (OwnedDinosaur record : records(player)) {
            if (!active.contains(record.id()) || record.id().equals(exceptId)) continue;
            FieldDodoEntity loaded = findLoaded(player.level().getServer(), record.id());
            if (loaded != null) {
                if (loaded.isWorkEnabled() && loaded.getWorkJobIndex() == 2
                        && loaded.getWorkWorkstationPositions().stream()
                        .map(pos -> canonicalTurbinePosition(player.level(), pos))
                        .anyMatch(station::equals)) return true;
                continue;
            }
            var input = TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    player.registryAccess(),
                    record.snapshot()
            );
            if (!input.getBooleanOr("PrimevalWorkEnabled", false)
                    || input.getIntOr("PrimevalWorkJob", -1) != 2) continue;
            if (input.listOrEmpty("PrimevalWorkstations", BlockPos.CODEC).stream()
                    .map(pos -> canonicalTurbinePosition(player.level(), pos))
                    .anyMatch(station::equals)) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos canonicalTurbinePosition(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.is(ModBlocks.TURBINE_PART.get())) {
            BlockPos master = TurbinePartBlock.masterPos(pos, state);
            if (TurbinePartBlock.isExpectedMaster(level, master, state)) return master.immutable();
        }
        return pos.immutable();
    }

    private static @Nullable FieldDodoEntity spawn(ServerLevel level, OwnedDinosaur record, BlockPos position) {
        FieldDodoEntity dinosaur = ModEntities.typeFor(record.species()).create(level, EntitySpawnReason.LOAD);
        if (dinosaur == null) return null;
        if (!record.snapshot().isEmpty()) {
            dinosaur.load(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    record.snapshot()
            ));
        }
        dinosaur.setUUID(record.id());
        dinosaur.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        boolean finishedRecovery = record.recoveryUntilTick() > 0L
                && record.recoveryUntilTick() <= level.getGameTime();
        float restoredHealth = finishedRecovery ? record.maxHealth() : record.health();
        dinosaur.restoreOwnedState(record.geneticQuality(), record.mutationMask(), record.hueVariant(),
                record.hunger(), record.mood(), restoredHealth, record.level());
        if (finishedRecovery) dinosaur.setHealth(dinosaur.getMaxHealth());
        dinosaur.setDeltaMovement(Vec3.ZERO);
        dinosaur.resetFallDistance();
        if (!record.name().isBlank()) dinosaur.setCustomName(net.minecraft.network.chat.Component.literal(record.name()));
        if (!level.addFreshEntity(dinosaur)) {
            dinosaur.discard();
            return null;
        }
        return dinosaur;
    }

    private static void moveToSlot(FieldDodoEntity dinosaur, BlockPos tablePos, int slot) {
        BlockPos position = slotPosition(tablePos, slot);
        dinosaur.teleportTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        dinosaur.setDeltaMovement(Vec3.ZERO);
        dinosaur.resetFallDistance();
    }

    private static void repositionLoadedCrew(ServerPlayer player, BlockPos tablePos, List<UUID> active) {
        for (int slot = 0; slot < active.size(); slot++) {
            FieldDodoEntity dinosaur = findLoaded(player.level().getServer(), active.get(slot));
            if (dinosaur != null) moveToSlot(dinosaur, tablePos, slot);
        }
    }

    private static BlockPos slotPosition(BlockPos tablePos, int slot) {
        int[][] offsets = {
                {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {3, 2}, {-3, 2}, {3, -2},
                {-3, -2}, {5, 0}, {-5, 0}, {0, 5}, {0, -5}, {5, 3}, {-5, 3}
        };
        int[] offset = offsets[Mth.clamp(slot, 0, offsets.length - 1)];
        return tablePos.offset(offset[0], 1, offset[1]);
    }

    private static OwnedDinosaur capture(FieldDodoEntity dinosaur) {
        String name = dinosaur.getDisplayName().getString();
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                dinosaur.registryAccess()
        );
        dinosaur.saveWithoutId(output);
        CompoundTag snapshot = output.buildResult();
        // A depot record is portable state, not a frozen world transform. Loading old
        // movement or identity data here caused recalled dinosaurs to jump, duplicate UUIDs,
        // or resume a stale fall before the table could place them safely.
        for (String transientKey : List.of("UUID", "Pos", "Motion", "Rotation")) {
            snapshot.remove(transientKey);
        }
        if (dinosaur.level() instanceof ServerLevel serverLevel) {
            snapshot.putString(SNAPSHOT_DIMENSION, serverLevel.dimension().identifier().toString());
            snapshot.putLong(SNAPSHOT_POSITION, dinosaur.blockPosition().asLong());
        }
        return new OwnedDinosaur(
                dinosaur.getUUID(), dinosaur.getSpecies(), name, dinosaur.getDinosaurLevel(),
                dinosaur.getHunger(), dinosaur.getMood(), dinosaur.getHealth(), dinosaur.getMaxHealth(),
                dinosaur.getGeneticQuality(), dinosaur.getMutationMask(), dinosaur.getHueVariant(), 0L,
                snapshot
        );
    }

    private static void returnCarriedCargo(FieldDodoEntity dinosaur) {
        ItemStack remainder = dinosaur.takeCarriedStackForStorage();
        if (remainder.isEmpty() || !(dinosaur.level() instanceof ServerLevel level)) return;
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : dinosaur.getWorkDestinationPositions()) if (!candidates.contains(pos)) candidates.add(pos);
        for (BlockPos pos : dinosaur.getWorkSourcePositions()) if (!candidates.contains(pos)) candidates.add(pos);
        for (BlockPos pos : candidates) {
            if (remainder.isEmpty()) break;
            Container container = level.isLoaded(pos) ? BaseInventoryIndex.containerAt(level, pos) : null;
            if (container != null) {
                remainder = insert(container, remainder);
            }
        }
        if (remainder.isEmpty()) return;
        BlockPos drop = dinosaur.getCommandTablePos().orElse(dinosaur.blockPosition());
        ItemEntity cargo = new ItemEntity(level, drop.getX() + 0.5D, drop.getY() + 1.15D,
                drop.getZ() + 0.5D, remainder);
        cargo.addTag("primeval_base_cargo");
        cargo.setDefaultPickUpDelay();
        level.addFreshEntity(cargo);
    }

    private static ItemStack insert(Container container, ItemStack offered) {
        ItemStack remainder = offered.copy();
        for (int slot = 0; slot < container.getContainerSize() && !remainder.isEmpty(); slot++) {
            ItemStack present = container.getItem(slot);
            if (present.isEmpty() || !ItemStack.isSameItemSameComponents(present, remainder)) continue;
            int room = Math.min(container.getMaxStackSize(remainder), present.getMaxStackSize()) - present.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, remainder.getCount());
            present.grow(moved);
            remainder.shrink(moved);
            container.setChanged();
        }
        for (int slot = 0; slot < container.getContainerSize() && !remainder.isEmpty(); slot++) {
            if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, remainder)) continue;
            int moved = Math.min(container.getMaxStackSize(remainder), remainder.getCount());
            container.setItem(slot, remainder.copyWithCount(moved));
            remainder.shrink(moved);
            container.setChanged();
        }
        return remainder;
    }

    private static Optional<OwnedDinosaur> find(List<OwnedDinosaur> records, UUID id) {
        return records.stream().filter(record -> record.id().equals(id)).findFirst();
    }

    private static void upsert(List<OwnedDinosaur> records, OwnedDinosaur replacement) {
        for (int index = 0; index < records.size(); index++) {
            if (records.get(index).id().equals(replacement.id())) {
                records.set(index, replacement);
                return;
            }
        }
        if (records.size() < MAX_OWNED) records.add(replacement);
    }

    private static void writeRecords(CompoundTag data, List<OwnedDinosaur> records) {
        int oldCount = Mth.clamp(data.getIntOr(OWNED_COUNT, 0), 0, MAX_OWNED);
        int count = Math.min(MAX_OWNED, records.size());
        data.putInt(OWNERSHIP_SCHEMA, CURRENT_SCHEMA);
        data.putInt(OWNED_COUNT, count);
        for (int index = 0; index < count; index++) {
            OwnedDinosaur record = records.get(index);
            String prefix = OWNED_PREFIX + index;
            data.putString(prefix + "Id", record.id().toString());
            data.putString(prefix + "Species", record.species().registryName());
            data.putString(prefix + "Name", record.name());
            data.putInt(prefix + "Level", record.level());
            data.putInt(prefix + "Hunger", record.hunger());
            data.putInt(prefix + "Mood", record.mood());
            data.putInt(prefix + "Health", Math.round(record.health() * 100.0F));
            data.putInt(prefix + "MaxHealth", Math.round(record.maxHealth() * 100.0F));
            data.putInt(prefix + "Quality", record.geneticQuality());
            data.putInt(prefix + "Mutations", record.mutationMask());
            data.putInt(prefix + "Hue", record.hueVariant());
            data.putLong(prefix + "RecoveryUntil", record.recoveryUntilTick());
            data.put(prefix + "State", record.snapshot());
        }
        for (int index = count; index < oldCount; index++) clearRecord(data, index);
    }

    private static void clearRecord(CompoundTag data, int index) {
        String prefix = OWNED_PREFIX + index;
        for (String suffix : List.of("Id", "Species", "Name", "Level", "Hunger", "Mood", "Health",
                "MaxHealth", "Quality", "Mutations", "Hue", "RecoveryUntil", "State")) data.remove(prefix + suffix);
    }

    private static void writeActive(CompoundTag data, List<UUID> ids) {
        int oldCount = Mth.clamp(data.getIntOr(ACTIVE_COUNT, 0), 0, ACTIVE_LIMIT);
        int count = Math.min(ACTIVE_LIMIT, ids.size());
        data.putInt(ACTIVE_COUNT, count);
        for (int index = 0; index < count; index++) data.putString(ACTIVE_PREFIX + index, ids.get(index).toString());
        for (int index = count; index < oldCount; index++) data.remove(ACTIVE_PREFIX + index);
    }

    public record OwnedDinosaur(UUID id, DinosaurSpecies species, String name, int level, int hunger, int mood,
                                float health, float maxHealth, int geneticQuality, int mutationMask, int hueVariant,
                                long recoveryUntilTick, CompoundTag snapshot) {
        public OwnedDinosaur {
            snapshot = snapshot == null ? new CompoundTag() : snapshot.copy();
        }

        @Override
        public CompoundTag snapshot() {
            return snapshot.copy();
        }

        public OwnedDinosaur withRecoveryUntil(long tick) {
            return new OwnedDinosaur(id, species, name, level, hunger, mood, health, maxHealth,
                    geneticQuality, mutationMask, hueVariant, Math.max(0L, tick), snapshot);
        }

        public boolean isOnExpedition(long gameTime) {
            return snapshot.getBooleanOr("PrimevalOnExpedition", false)
                    && snapshot.getLongOr("PrimevalExpeditionEnd", 0L) > gameTime;
        }

        public int expeditionTier() {
            return Mth.clamp(snapshot.getIntOr("PrimevalExpeditionTier", 0), 0, 4);
        }

        public long expeditionTicksRemaining(long gameTime) {
            return isOnExpedition(gameTime)
                    ? Math.max(0L, snapshot.getLongOr("PrimevalExpeditionEnd", 0L) - gameTime)
                    : 0L;
        }
    }

    public record SwapResult(boolean success, String message) {
    }

    public static long recoveryDurationTicks(DinosaurSpecies species) {
        long base = switch (species) {
            case DODO, VELOCIRAPTOR, DILOPHOSAURUS, PACHYCEPHALOSAURUS -> 3L * 60L * 20L;
            case TRICERATOPS, STEGOSAURUS, PARASAUROLOPHUS, ANKYLOSAURUS, PTERANODON -> 5L * 60L * 20L;
            case TYRANNOSAURUS, BRACHIOSAURUS, SPINOSAURUS -> 8L * 60L * 20L;
        };
        return Math.max(1L, Math.round(base * PrimevalTuning.server().recoveryTime()));
    }
}
