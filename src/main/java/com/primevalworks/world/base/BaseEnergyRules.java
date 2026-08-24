package com.primevalworks.world.base;

import com.primevalworks.registry.ModBlocks;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.block.entity.AncientFurnaceBlockEntity;
import com.primevalworks.world.block.entity.ActiveEnergyConsumer;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class BaseEnergyRules {
    public static final int PROCESSOR_DEMAND = 6;
    private static final Map<net.minecraft.world.level.Level, Map<BlockPos, BlockPos>> NETWORK_BINDINGS =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<net.minecraft.world.level.Level, Set<BlockPos>> LOADED_TABLES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private BaseEnergyRules() {
    }

    public static boolean isGenerator(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.WIND_TURBINE.get() || block == ModBlocks.WATER_TURBINE.get();
    }

    public static int demandPerSecond(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.LASER_OBSERVER.get()
                || block == ModBlocks.ENHANCED_RAIL.get()) {
            return 1;
        }
        if (block == ModBlocks.REINFORCED_PISTON.get()
                || block == ModBlocks.STICKY_REINFORCED_PISTON.get()) {
            return 2;
        }
        if (block == ModBlocks.PREMIUM_EGG_INCUBATOR.get()) {
            return 2;
        }
        if (block == ModBlocks.ANCIENT_FURNACE.get()) return 3;
        if (block == ModBlocks.DART_TURRET.get()) {
            return 3;
        }
        if (block == ModBlocks.ANCIENT_SPELL_STONE.get()) {
            return 4;
        }
        if (block == ModBlocks.MAGIC_TURRET.get()) return 5;
        if (block == ModBlocks.PROCESSOR.get()) {
            return PROCESSOR_DEMAND;
        }
        return 0;
    }

    public static float demandPerSecond(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level != null && level.isLoaded(pos)
                && level.getBlockEntity(pos) instanceof AncientFurnaceBlockEntity furnace) {
            return furnace.energyPerSecond();
        }
        return demandPerSecond(level.getBlockState(pos));
    }

    public static CommandTableBlockEntity ownedTableFor(ServerPlayer player, BlockPos consumerPos) {
        CommandTableBlock.ClaimedTable claimed = CommandTableBlock.getClaimedTable(player).orElse(null);
        if (claimed == null || claimed.level() != player.level()) return null;
        CommandTableBlockEntity table = CommandTableBlock.tableEntity(claimed.level(), claimed.pos());
        if (table == null || !table.isOwnedBy(player.getUUID())) return null;
        return consumerPos.distSqr(claimed.pos()) <= (double)table.baseRadius() * table.baseRadius() ? table : null;
    }

    public static boolean isPowered(ServerPlayer player, BlockPos consumerPos) {
        CommandTableBlockEntity table = ownedTableFor(player, consumerPos);
        return table != null && table.isEnergyConsumerPowered(consumerPos);
    }

    public static boolean isPowered(net.minecraft.world.level.Level level, BlockPos tablePos, BlockPos consumerPos) {
        CommandTableBlockEntity table = CommandTableBlock.tableEntity(level, tablePos);
        return table != null && table.isEnergyConsumerPowered(consumerPos);
    }

    public static boolean isPowered(net.minecraft.world.level.Level level, BlockPos consumerPos) {
        CommandTableBlockEntity table = boundTable(level, consumerPos);
        return table != null && table.isEnergyConsumerPowered(consumerPos);
    }

    public static boolean isConnected(net.minecraft.world.level.Level level, BlockPos consumerPos) {
        CommandTableBlockEntity table = boundTable(level, consumerPos);
        return table != null && table.isEnergyConsumerEnabled(consumerPos);
    }

    public static boolean ownsPosition(net.minecraft.world.level.Level level, BlockPos consumerPos, Vec3 position) {
        CommandTableBlockEntity table = boundTable(level, consumerPos);
        if (table == null) return false;
        BlockPos ownPos = table.getBlockPos();
        double ownDistance = position.distanceToSqr(Vec3.atCenterOf(ownPos));
        if (ownDistance > (double)table.baseRadius() * table.baseRadius()) return false;

        Set<BlockPos> loadedTables;
        synchronized (LOADED_TABLES) {
            loadedTables = Set.copyOf(LOADED_TABLES.getOrDefault(level, Set.of()));
        }
        long ownKey = ownPos.asLong();
        for (BlockPos otherPos : loadedTables) {
            if (otherPos.equals(ownPos)) continue;
            double otherDistance = position.distanceToSqr(Vec3.atCenterOf(otherPos));
            if (otherDistance < ownDistance || otherDistance == ownDistance && otherPos.asLong() < ownKey) {
                return false;
            }
        }
        return true;
    }

    public static float activeDemandPerSecond(net.minecraft.world.level.Level level, BlockPos pos) {
        float ratedDemand = demandPerSecond(level, pos);
        if (ratedDemand <= 0.0F || !level.isLoaded(pos)) return 0.0F;
        if (level.getBlockEntity(pos) instanceof ActiveEnergyConsumer consumer
                && !consumer.requestsBaseEnergy(level)) return 0.0F;
        return ratedDemand;
    }

    public static CommandTableBlockEntity boundTable(net.minecraft.world.level.Level level, BlockPos consumerPos) {
        BlockPos tablePos;
        synchronized (NETWORK_BINDINGS) {
            tablePos = NETWORK_BINDINGS.getOrDefault(level, Map.of()).get(consumerPos);
        }
        CommandTableBlockEntity bound = tablePos == null ? null : CommandTableBlock.tableEntity(level, tablePos);
        if (bound != null && bound.isEnergyConsumerEnabled(consumerPos)) return bound;

        // Bindings are only a runtime index; the Command Table's saved consumer set is
        // authoritative. Rebuild a missing entry after chunk load instead of leaving a
        // connected machine silently unpowered for the rest of the session.
        Set<BlockPos> loadedTables;
        synchronized (LOADED_TABLES) {
            loadedTables = Set.copyOf(LOADED_TABLES.getOrDefault(level, Set.of()));
        }
        for (BlockPos candidatePos : loadedTables) {
            CommandTableBlockEntity candidate = CommandTableBlock.tableEntity(level, candidatePos);
            if (candidate != null && candidate.isEnergyConsumerEnabled(consumerPos)) {
                bindConsumer(level, candidatePos, consumerPos, true);
                return candidate;
            }
        }
        if (tablePos != null) bindConsumer(level, tablePos, consumerPos, false);
        return null;
    }

    public static void registerLoadedTable(net.minecraft.world.level.Level level, BlockPos tablePos) {
        synchronized (LOADED_TABLES) {
            LOADED_TABLES.computeIfAbsent(level, ignored -> new HashSet<>()).add(tablePos.immutable());
        }
    }

    public static void unregisterLoadedTable(net.minecraft.world.level.Level level, BlockPos tablePos) {
        synchronized (LOADED_TABLES) {
            Set<BlockPos> tables = LOADED_TABLES.get(level);
            if (tables == null) return;
            tables.remove(tablePos);
            if (tables.isEmpty()) LOADED_TABLES.remove(level);
        }
    }

    /** Uses the small persisted consumer index instead of scanning thousands of world blocks on every mob spawn. */
    public static boolean hasPoweredBlockNearby(net.minecraft.world.level.Level level, BlockPos center,
                                                Block wanted, int radius) {
        double radiusSquared = (double)radius * radius;
        Map<BlockPos, BlockPos> bindings;
        synchronized (NETWORK_BINDINGS) {
            bindings = Map.copyOf(NETWORK_BINDINGS.getOrDefault(level, Map.of()));
        }
        for (Map.Entry<BlockPos, BlockPos> entry : bindings.entrySet()) {
            BlockPos consumer = entry.getKey();
            if (consumer.distSqr(center) > radiusSquared || !level.isLoaded(consumer)) continue;
            if (level.getBlockState(consumer).is(wanted)
                    && isPowered(level, entry.getValue(), consumer)) return true;
        }
        return false;
    }

    public static List<BlockPos> poweredConsumers(net.minecraft.world.level.Level level, Block wanted) {
        Map<BlockPos, BlockPos> bindings;
        synchronized (NETWORK_BINDINGS) {
            bindings = Map.copyOf(NETWORK_BINDINGS.getOrDefault(level, Map.of()));
        }
        List<BlockPos> result = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockPos> entry : bindings.entrySet()) {
            BlockPos consumer = entry.getKey();
            if (level.isLoaded(consumer) && level.getBlockState(consumer).is(wanted)
                    && isPowered(level, entry.getValue(), consumer)) result.add(consumer);
        }
        return List.copyOf(result);
    }

    public static void bindConsumer(net.minecraft.world.level.Level level, BlockPos tablePos,
                                    BlockPos consumerPos, boolean enabled) {
        synchronized (NETWORK_BINDINGS) {
            Map<BlockPos, BlockPos> bindings = NETWORK_BINDINGS.computeIfAbsent(level, ignored -> new HashMap<>());
            if (enabled) bindings.put(consumerPos.immutable(), tablePos.immutable());
            else bindings.remove(consumerPos);
            if (bindings.isEmpty()) NETWORK_BINDINGS.remove(level);
        }
    }

    public static Component unavailableMessage(ServerPlayer player, BlockPos consumerPos) {
        CommandTableBlockEntity table = ownedTableFor(player, consumerPos);
        if (table == null) return Component.literal("This block needs a Command Table in range.");
        if (!table.isEnergyConsumerEnabled(consumerPos)) {
            return Component.literal("This block needs energy. Connect it from the Command Table's Energy Map.");
        }
        return Component.literal("This block is connected, but the base has no stored energy.");
    }
}
