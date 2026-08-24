package com.primevalworks.world.work;

import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.block.entity.FoodBoxBlockEntity;
import com.primevalworks.world.inventory.AutomationConfigurableContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A bounded, loaded-chunk-only view of storage connected to one Command Table. */
public final class BaseInventoryIndex {
    public static final int MAX_CONTAINERS = 512;
    public static final int MAX_DISTINCT_ITEMS_PER_CONTAINER = 128;

    private BaseInventoryIndex() {
    }

    public static List<IndexedContainer> scan(ServerLevel level, BlockPos tablePos, int radius) {
        Map<BlockPos, IndexedContainer> indexed = new LinkedHashMap<>();
        List<BlockPos> nearbyTables = new ArrayList<>();
        int minimumChunkX = (tablePos.getX() - radius) >> 4;
        int maximumChunkX = (tablePos.getX() + radius) >> 4;
        int minimumChunkZ = (tablePos.getZ() - radius) >> 4;
        int maximumChunkZ = (tablePos.getZ() + radius) >> 4;
        double radiusSquared = (double) radius * radius;

        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (entry.getValue() instanceof CommandTableBlockEntity) {
                        nearbyTables.add(entry.getKey().immutable());
                    }
                }
            }
        }

        outer:
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (BlockPos rawPos : chunk.getBlockEntities().keySet()) {
                    BlockPos pos = canonicalPosition(level, rawPos);
                    if (pos.distSqr(tablePos) > radiusSquared || indexed.containsKey(pos)) continue;
                    if (!belongsToTable(pos, tablePos, nearbyTables)) continue;
                    Container container = containerAt(level, pos);
                    if (container == null) continue;
                    indexed.put(pos, inspect(pos, container));
                    if (indexed.size() >= MAX_CONTAINERS) break outer;
                }
            }
        }
        return List.copyOf(indexed.values());
    }

    private static boolean belongsToTable(BlockPos containerPos, BlockPos tablePos, List<BlockPos> nearbyTables) {
        double ownDistance = containerPos.distSqr(tablePos);
        long ownKey = tablePos.asLong();
        for (BlockPos otherTable : nearbyTables) {
            if (otherTable.equals(tablePos)) continue;
            double otherDistance = containerPos.distSqr(otherTable);
            if (otherDistance < ownDistance || otherDistance == ownDistance && otherTable.asLong() < ownKey) {
                return false;
            }
        }
        return true;
    }

    public static BlockPos canonicalPosition(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return pos.immutable();
        }
        BlockPos connected = ChestBlock.getConnectedBlockPos(pos, state);
        return pos.asLong() <= connected.asLong() ? pos.immutable() : connected.immutable();
    }

    public static @Nullable Container containerAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            Container combined = ChestBlock.getContainer(chest, state, level, pos, true);
            if (combined != null) return combined;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container container ? container : null;
    }

    public static boolean canExtract(Container container, int slot, ItemStack stack) {
        if (!container.canTakeItem(container, slot, stack)) return false;
        if (!(container instanceof WorldlyContainer sided)) return true;
        for (Direction direction : Direction.values()) {
            if (containsSlot(sided.getSlotsForFace(direction), slot)
                    && sided.canTakeItemThroughFace(slot, stack, direction)) return true;
        }
        return false;
    }

    public static boolean canInsert(Container container, int slot, ItemStack stack) {
        if (!container.canPlaceItem(slot, stack)) return false;
        if (!(container instanceof WorldlyContainer sided)) return true;
        for (Direction direction : Direction.values()) {
            if (containsSlot(sided.getSlotsForFace(direction), slot)
                    && sided.canPlaceItemThroughFace(slot, stack, direction)) return true;
        }
        return false;
    }

    private static IndexedContainer inspect(BlockPos pos, Container container) {
        Map<Identifier, MutableItem> items = new LinkedHashMap<>();
        boolean acceptsAny = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                acceptsAny |= acceptsRepresentativeItem(container, slot);
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            MutableItem item = items.computeIfAbsent(id, ignored -> new MutableItem());
            if (canExtract(container, slot, stack)) item.extractableCount += stack.getCount();
            if (canInsert(container, slot, stack)
                    && stack.getCount() < Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize())) {
                item.acceptsMore = true;
            }
        }
        List<IndexedItem> contents = new ArrayList<>(Math.min(items.size(), MAX_DISTINCT_ITEMS_PER_CONTAINER));
        for (Map.Entry<Identifier, MutableItem> entry : items.entrySet()) {
            if (contents.size() >= MAX_DISTINCT_ITEMS_PER_CONTAINER) break;
            MutableItem value = entry.getValue();
            contents.add(new IndexedItem(entry.getKey(), value.extractableCount, value.acceptsMore));
        }
        return new IndexedContainer(
                pos.immutable(),
                acceptsAny,
                canSupplyItems(container),
                canReceiveItems(container),
                List.copyOf(contents),
                container
        );
    }

    /**
     * Reports whether an empty container can become an automation source later. This is
     * deliberately separate from its current contents: players must be able to wire a
     * furnace or Processor output before the first item has finished.
     */
    private static boolean canSupplyItems(Container container) {
        if (container instanceof FoodBoxBlockEntity) return false;
        if (container instanceof AutomationConfigurableContainer configurable) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (configurable.allowsAutomationExtract(slot)) return true;
            }
            return false;
        }
        if (container instanceof AbstractFurnaceBlockEntity) return true;
        if (!(container instanceof WorldlyContainer sided)) return container.getContainerSize() > 0;
        for (Direction direction : Direction.values()) {
            if (sided.getSlotsForFace(direction).length > 0) return true;
        }
        return false;
    }

    /** Same idea as {@link #canSupplyItems(Container)}, but for future insert routes. */
    private static boolean canReceiveItems(Container container) {
        if (container instanceof AutomationConfigurableContainer configurable) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (configurable.allowsAutomationInsert(slot)) return true;
            }
            return false;
        }
        if (!(container instanceof WorldlyContainer sided)) return container.getContainerSize() > 0;
        for (Direction direction : Direction.values()) {
            if (sided.getSlotsForFace(direction).length > 0) return true;
        }
        return false;
    }

    private static boolean acceptsRepresentativeItem(Container container, int slot) {
        // Empty ordinary storage slots accept any item. Sided/machine inventories are reported
        // conservatively and still get exact per-item checks from occupied slots.
        return !(container instanceof WorldlyContainer) && container.canPlaceItem(slot, ItemStack.EMPTY);
    }

    private static boolean containsSlot(int[] slots, int target) {
        for (int slot : slots) if (slot == target) return true;
        return false;
    }

    private static final class MutableItem {
        private int extractableCount;
        private boolean acceptsMore;
    }

    public record IndexedContainer(BlockPos pos, boolean acceptsAnyItem, boolean canSupplyItems,
                                   boolean canReceiveItems, List<IndexedItem> items, Container container) {
        public int extractableCount(Identifier identifier) {
            return items.stream().filter(item -> item.identifier().equals(identifier))
                    .mapToInt(IndexedItem::extractableCount).sum();
        }

        public boolean accepts(Identifier identifier) {
            return acceptsAnyItem || items.stream().anyMatch(item ->
                    item.identifier().equals(identifier) && item.acceptsMore());
        }
    }

    public record IndexedItem(Identifier identifier, int extractableCount, boolean acceptsMore) {
    }
}
