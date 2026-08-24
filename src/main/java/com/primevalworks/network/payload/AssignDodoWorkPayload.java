package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.block.TurbinePartBlock;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.work.WorkSpecialtyRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AssignDodoWorkPayload(
        int entityId,
        int jobIndex,
        BlockPos commandTablePos,
        List<BlockPos> sourcePositions,
        List<BlockPos> workstationPositions,
        List<BlockPos> destinationPositions,
        Optional<BlockPos> areaEndPos,
        List<BlockPos> fallbackPositions,
        List<String> itemFilters,
        List<String> fuelFilters,
        Map<BlockPos, Integer> blockPriorities,
        int expeditionTier,
        int priority,
        int batchSize,
        int schedule,
        int sourceReserve,
        int destinationTarget,
        int repeatMode,
        int routePolicy,
        boolean exactItemMatch,
        boolean avoidDanger
) implements CustomPacketPayload {
    public static final Type<AssignDodoWorkPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "assign_dodo_work")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AssignDodoWorkPayload> STREAM_CODEC = StreamCodec.of(
            AssignDodoWorkPayload::encode,
            AssignDodoWorkPayload::decode
    );

    static void encode(RegistryFriendlyByteBuf buffer, AssignDodoWorkPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeVarInt(payload.jobIndex);
        buffer.writeLong(payload.commandTablePos.asLong());
        writePositions(buffer, payload.sourcePositions);
        writePositions(buffer, payload.workstationPositions);
        writePositions(buffer, payload.destinationPositions);
        writePos(buffer, payload.areaEndPos);
        writePositions(buffer, payload.fallbackPositions);
        int itemCount = Math.min(16, payload.itemFilters.size());
        buffer.writeVarInt(itemCount);
        payload.itemFilters.stream().limit(itemCount).forEach(value -> buffer.writeUtf(value, 128));
        int fuelCount = Math.min(16, payload.fuelFilters.size());
        buffer.writeVarInt(fuelCount);
        payload.fuelFilters.stream().limit(fuelCount).forEach(value -> buffer.writeUtf(value, 128));
        writeBlockPriorities(buffer, payload.blockPriorities);
        buffer.writeVarInt(payload.expeditionTier);
        buffer.writeVarInt(payload.priority);
        buffer.writeVarInt(payload.batchSize);
        buffer.writeVarInt(payload.schedule);
        buffer.writeVarInt(payload.sourceReserve);
        buffer.writeVarInt(payload.destinationTarget);
        buffer.writeVarInt(payload.repeatMode);
        buffer.writeVarInt(payload.routePolicy);
        buffer.writeBoolean(payload.exactItemMatch);
        buffer.writeBoolean(payload.avoidDanger);
    }

    static AssignDodoWorkPayload decode(RegistryFriendlyByteBuf buffer) {
        return new AssignDodoWorkPayload(
                buffer.readVarInt(),
                buffer.readVarInt(),
                BlockPos.of(buffer.readLong()),
                readPositions(buffer),
                readPositions(buffer),
                readPositions(buffer),
                readPos(buffer),
                readPositions(buffer),
                readStrings(buffer),
                readStrings(buffer),
                readBlockPriorities(buffer),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    private static void writePos(RegistryFriendlyByteBuf buffer, Optional<BlockPos> position) {
        buffer.writeBoolean(position.isPresent());
        position.ifPresent(value -> buffer.writeLong(value.asLong()));
    }

    private static Optional<BlockPos> readPos(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(BlockPos.of(buffer.readLong())) : Optional.empty();
    }

    private static void writePositions(RegistryFriendlyByteBuf buffer, List<BlockPos> positions) {
        buffer.writeVarInt(Math.min(8, positions.size()));
        positions.stream().limit(8).forEach(pos -> buffer.writeLong(pos.asLong()));
    }

    private static List<BlockPos> readPositions(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > 8) {
            throw new IllegalArgumentException("Invalid work position count: " + size);
        }
        List<BlockPos> positions = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            positions.add(BlockPos.of(buffer.readLong()));
        }
        return positions;
    }

    private static List<String> readStrings(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > 16) {
            throw new IllegalArgumentException("Invalid work item count: " + size);
        }
        List<String> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            values.add(buffer.readUtf(128));
        }
        return values;
    }

    private static void writeBlockPriorities(RegistryFriendlyByteBuf buffer, Map<BlockPos, Integer> priorities) {
        int count = Math.min(33, priorities.size());
        buffer.writeVarInt(count);
        priorities.entrySet().stream().limit(count).forEach(entry -> {
            buffer.writeLong(entry.getKey().asLong());
            buffer.writeVarInt(Mth.clamp(entry.getValue(), 0, 3));
        });
    }

    private static Map<BlockPos, Integer> readBlockPriorities(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > 33) {
            throw new IllegalArgumentException("Invalid block priority count: " + size);
        }
        Map<BlockPos, Integer> priorities = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            priorities.put(BlockPos.of(buffer.readLong()), Mth.clamp(buffer.readVarInt(), 0, 3));
        }
        return priorities;
    }

    public static void handle(AssignDodoWorkPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        String error = validationError(player, payload);
        if (error != null) {
            player.sendOverlayMessage(Component.literal(error));
            return;
        }
        FieldDodoEntity dodo = (FieldDodoEntity) player.level().getEntity(payload.entityId);
        List<BlockPos> workstationPositions = payload.jobIndex == 2
                ? canonicalTurbinePositions(player, payload.workstationPositions)
                : payload.workstationPositions;
        Map<BlockPos, Integer> blockPriorities = payload.jobIndex == 2
                ? canonicalTurbinePriorities(player, payload.blockPriorities)
                : payload.blockPriorities;

        dodo.assignWork(
                payload.jobIndex,
                payload.commandTablePos,
                payload.sourcePositions,
                workstationPositions,
                payload.destinationPositions,
                payload.areaEndPos.orElse(null),
                payload.fallbackPositions,
                payload.itemFilters,
                payload.fuelFilters,
                blockPriorities,
                Mth.clamp(payload.expeditionTier, 0, 4),
                Mth.clamp(payload.priority, 0, 3),
                Mth.clamp(payload.batchSize, 1, 64),
                Mth.clamp(payload.schedule, 0, 2),
                Mth.clamp(payload.sourceReserve, 0, 4096),
                Mth.clamp(payload.destinationTarget, 0, 4096),
                Mth.clamp(payload.repeatMode, 0, 2),
                Mth.clamp(payload.routePolicy, 0, 2),
                payload.exactItemMatch,
                payload.avoidDanger
        );
        PacketDistributor.sendToPlayer(player, DinosaurWorkStatePayload.from(dodo));
        player.sendOverlayMessage(Component.literal("Work order saved: " + jobName(payload.jobIndex) + "."));
    }

    private static String validationError(ServerPlayer player, AssignDodoWorkPayload payload) {
        if (!(player.level().getEntity(payload.entityId) instanceof FieldDodoEntity dodo)) {
            return "That dinosaur is no longer available. Reopen the Command Table and try again.";
        }
        if (!dodo.isAlive()) return "That dinosaur is recovering and cannot take a work order yet.";
        if (dodo.isOnExpedition()) return "That dinosaur is already away on an expedition.";
        var table = CommandTableBlock.tableEntity(player.level(), payload.commandTablePos);
        if (table == null) return "The Command Table could not be found. Move closer and reopen it.";
        if (!table.isOwnedBy(player.getUUID())) return "This Command Table does not belong to you.";
        if (!dodo.isOwnedBy(player.getUUID())) return "You can only assign work to your own dinosaurs.";
        if (player.distanceToSqr(payload.commandTablePos.getCenter()) > 4096.0D) {
            return "You are too far from the Command Table to save this order.";
        }
        double dinosaurRange = table.baseRadius() + 4.0D;
        if (dodo.distanceToSqr(payload.commandTablePos.getCenter()) > dinosaurRange * dinosaurRange) {
            return "That dinosaur is outside this base's work range.";
        }
        if (payload.jobIndex < 0 || payload.jobIndex > 4) return "That work specialty does not exist.";
        if (payload.jobIndex == 4 && !WorkSpecialtyRules.canAttemptExpedition(
                payload.expeditionTier, dodo.getSpecialtyStars(4))) {
            return "Primordial Frontier requires at least a two-star expedition dinosaur.";
        }
        int baseRadius = table.baseRadius();
        if (!insideBase(payload.commandTablePos, payload.sourcePositions, baseRadius)
                || !insideBase(payload.commandTablePos, payload.workstationPositions, baseRadius)
                || !insideBase(payload.commandTablePos, payload.destinationPositions, baseRadius)
                || !insideBase(payload.commandTablePos, payload.areaEndPos, baseRadius)
                || !insideBase(payload.commandTablePos, payload.fallbackPositions, baseRadius)) {
            return "One of the selected blocks is outside this base's range.";
        }
        if (!validSpecialtySelections(player, payload)) return specialtySelectionError(payload.jobIndex);
        if (payload.jobIndex == 2 && !energyStationsAreAvailable(player, payload)) {
            return "That turbine already has an energy worker. Choose another source.";
        }
        if (!validItemRules(player, payload)) return "The selected item rules are invalid for this specialty.";
        if (!validBlockPriorities(payload, baseRadius)) return "A block priority no longer matches this work order.";
        return null;
    }

    private static String specialtySelectionError(int jobIndex) {
        return switch (jobIndex) {
            case 0 -> "Transport needs at least one valid source and one valid destination.";
            case 1 -> "Fire work can only be assigned to furnaces, Ancient Furnaces and Processors.";
            case 2 -> "Energy work can only be assigned to wind or water turbines.";
            case 3 -> "Crafting needs a normal crafting table and one recipe result.";
            case 4 -> "Expeditions do not use blocks or item filters.";
            default -> "This work order is invalid.";
        };
    }

    private static String jobName(int jobIndex) {
        return switch (jobIndex) {
            case 0 -> "transport";
            case 1 -> "fire";
            case 2 -> "energy";
            case 3 -> "crafting";
            case 4 -> "expedition";
            default -> "work";
        };
    }

    private static boolean insideBase(BlockPos table, Optional<BlockPos> candidate, int baseRadius) {
        if (candidate.isEmpty()) {
            return true;
        }
        BlockPos pos = candidate.get();
        return pos.distSqr(table) <= (double)baseRadius * baseRadius;
    }

    private static boolean insideBase(BlockPos table, List<BlockPos> candidates, int baseRadius) {
        return candidates.size() <= 8
                && candidates.stream().allMatch(pos -> insideBase(table, Optional.of(pos), baseRadius));
    }

    private static boolean validBlockPriorities(AssignDodoWorkPayload payload, int baseRadius) {
        if (payload.blockPriorities.size() > 33) {
            return false;
        }
        List<BlockPos> selected = new ArrayList<>();
        selected.addAll(payload.sourcePositions);
        selected.addAll(payload.workstationPositions);
        selected.addAll(payload.destinationPositions);
        selected.addAll(payload.fallbackPositions);
        payload.areaEndPos.ifPresent(selected::add);
        return payload.blockPriorities.entrySet().stream().allMatch(entry ->
                entry.getValue() >= 0
                        && entry.getValue() <= 3
                        && selected.contains(entry.getKey())
                        && insideBase(payload.commandTablePos, Optional.of(entry.getKey()), baseRadius));
    }

    private static boolean validSpecialtySelections(ServerPlayer player, AssignDodoWorkPayload payload) {
        return switch (payload.jobIndex) {
            case 0 -> !payload.sourcePositions.isEmpty()
                    && !payload.destinationPositions.isEmpty()
                    && payload.sourcePositions.stream().allMatch(pos -> isTransportSource(player, pos))
                    && payload.destinationPositions.stream().allMatch(pos -> isContainer(player, pos))
                    && payload.workstationPositions.isEmpty()
                    && payload.fallbackPositions.isEmpty();
            case 1 -> !payload.workstationPositions.isEmpty()
                    && payload.workstationPositions.stream().allMatch(pos ->
                    player.level().getBlockState(pos).getBlock() instanceof AbstractFurnaceBlock
                            || player.level().getBlockState(pos).is(ModBlocks.PROCESSOR.get()))
                    && payload.sourcePositions.isEmpty()
                    && payload.destinationPositions.isEmpty()
                    && payload.fallbackPositions.isEmpty();
            case 2 -> canonicalTurbinePositions(player, payload.workstationPositions).size() == 1
                    && canonicalTurbinePositions(player, payload.workstationPositions).stream().allMatch(pos -> {
                        var block = player.level().getBlockState(pos).getBlock();
                        return block == ModBlocks.WIND_TURBINE.get() || block == ModBlocks.WATER_TURBINE.get();
                    })
                    && payload.sourcePositions.isEmpty()
                    && payload.destinationPositions.isEmpty()
                    && payload.fallbackPositions.isEmpty();
            case 3 -> !payload.workstationPositions.isEmpty()
                    && payload.workstationPositions.stream().allMatch(pos ->
                    player.level().getBlockState(pos).getBlock() instanceof CraftingTableBlock)
                    && payload.sourcePositions.isEmpty()
                    && payload.destinationPositions.isEmpty()
                    && payload.fallbackPositions.isEmpty();
            case 4 -> payload.sourcePositions.isEmpty()
                    && payload.workstationPositions.isEmpty()
                    && payload.destinationPositions.isEmpty()
                    && payload.fallbackPositions.isEmpty()
                    && payload.areaEndPos.isEmpty();
            default -> false;
        };
    }

    private static boolean isTransportSource(ServerPlayer player, BlockPos pos) {
        return isContainer(player, pos)
                || player.level().getBlockState(pos).getBlock() instanceof CraftingTableBlock;
    }

    private static boolean isContainer(ServerPlayer player, BlockPos pos) {
        return player.level().getBlockEntity(pos) instanceof Container;
    }

    private static boolean validItemRules(ServerPlayer player, AssignDodoWorkPayload payload) {
        int itemLimit = WorkSpecialtyRules.itemFilterCapacity(payload.jobIndex);
        if (payload.itemFilters.size() > itemLimit
                || payload.fuelFilters.size() > WorkSpecialtyRules.fuelFilterCapacity(payload.jobIndex)) {
            return false;
        }
        if (!payload.itemFilters.stream().allMatch(AssignDodoWorkPayload::isKnownItem)) {
            return false;
        }
        return payload.fuelFilters.stream().allMatch(value -> {
            Identifier id = Identifier.tryParse(value);
            if (id == null) {
                return false;
            }
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id).orElse(null);
            return item != null && player.level().fuelValues().isFuel(new ItemStack(item));
        });
    }

    private static boolean isKnownItem(String value) {
        Identifier id = Identifier.tryParse(value);
        return id != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id).isPresent();
    }

    private static List<BlockPos> canonicalTurbinePositions(ServerPlayer player, List<BlockPos> positions) {
        return positions.stream()
                .map(pos -> canonicalTurbinePosition(player, pos))
                .distinct()
                .limit(8)
                .toList();
    }

    private static Map<BlockPos, Integer> canonicalTurbinePriorities(
            ServerPlayer player,
            Map<BlockPos, Integer> priorities
    ) {
        Map<BlockPos, Integer> result = new LinkedHashMap<>();
        priorities.forEach((pos, priority) -> result.merge(
                canonicalTurbinePosition(player, pos),
                Mth.clamp(priority, 0, 3),
                Math::max
        ));
        return Map.copyOf(result);
    }

    private static BlockPos canonicalTurbinePosition(ServerPlayer player, BlockPos pos) {
        var state = player.level().getBlockState(pos);
        if (state.is(ModBlocks.TURBINE_PART.get())) {
            BlockPos master = TurbinePartBlock.masterPos(pos, state);
            if (TurbinePartBlock.isExpectedMaster(player.level(), master, state)) return master.immutable();
        }
        return pos.immutable();
    }

    private static boolean energyStationsAreAvailable(ServerPlayer player, AssignDodoWorkPayload payload) {
        List<BlockPos> requested = canonicalTurbinePositions(player, payload.workstationPositions);
        if (!(player.level().getEntity(payload.entityId) instanceof FieldDodoEntity assigned)) return false;
        return requested.stream().noneMatch(pos ->
                DinosaurOwnership.hasEnergyStationAssignment(player, pos, assigned.getUUID()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
