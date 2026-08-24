package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.WorksitePlannerScreen;
import com.primevalworks.world.work.BaseInventoryIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record BaseInventoryPayload(BlockPos commandTablePos, List<ContainerEntry> containers)
        implements CustomPacketPayload {
    private static final int MAX_TOTAL_ITEMS = 4096;
    public static final Type<BaseInventoryPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "base_inventory")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseInventoryPayload> STREAM_CODEC = StreamCodec.of(
            BaseInventoryPayload::encode,
            BaseInventoryPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, BaseInventoryPayload payload) {
        buffer.writeLong(payload.commandTablePos.asLong());
        int containerCount = Math.min(BaseInventoryIndex.MAX_CONTAINERS, payload.containers.size());
        buffer.writeVarInt(containerCount);
        int remainingItems = MAX_TOTAL_ITEMS;
        for (ContainerEntry container : payload.containers.subList(0, containerCount)) {
            buffer.writeLong(container.pos.asLong());
            buffer.writeBoolean(container.acceptsAnyItem);
            buffer.writeBoolean(container.canSupplyItems);
            buffer.writeBoolean(container.canReceiveItems);
            int itemCount = Math.min(Math.min(BaseInventoryIndex.MAX_DISTINCT_ITEMS_PER_CONTAINER,
                    container.items.size()), remainingItems);
            buffer.writeVarInt(itemCount);
            for (ItemEntry item : container.items.subList(0, itemCount)) {
                buffer.writeUtf(item.identifier, 128);
                buffer.writeVarInt(Math.max(0, item.extractableCount));
                buffer.writeBoolean(item.acceptsMore);
            }
            remainingItems -= itemCount;
        }
    }

    private static BaseInventoryPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos tablePos = BlockPos.of(buffer.readLong());
        int containerCount = buffer.readVarInt();
        if (containerCount < 0 || containerCount > BaseInventoryIndex.MAX_CONTAINERS) {
            throw new IllegalArgumentException("Invalid base container count: " + containerCount);
        }
        int totalItems = 0;
        List<ContainerEntry> containers = new ArrayList<>(containerCount);
        for (int containerIndex = 0; containerIndex < containerCount; containerIndex++) {
            BlockPos pos = BlockPos.of(buffer.readLong());
            boolean acceptsAny = buffer.readBoolean();
            boolean canSupply = buffer.readBoolean();
            boolean canReceive = buffer.readBoolean();
            int itemCount = buffer.readVarInt();
            totalItems += itemCount;
            if (itemCount < 0 || itemCount > BaseInventoryIndex.MAX_DISTINCT_ITEMS_PER_CONTAINER
                    || totalItems > MAX_TOTAL_ITEMS) {
                throw new IllegalArgumentException("Invalid base inventory item count: " + itemCount);
            }
            List<ItemEntry> items = new ArrayList<>(itemCount);
            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                items.add(new ItemEntry(buffer.readUtf(128), buffer.readVarInt(), buffer.readBoolean()));
            }
            containers.add(new ContainerEntry(pos, acceptsAny, canSupply, canReceive, List.copyOf(items)));
        }
        return new BaseInventoryPayload(tablePos, List.copyOf(containers));
    }

    public static void handle(BaseInventoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WorksitePlannerScreen.acceptBaseInventory(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ContainerEntry(BlockPos pos, boolean acceptsAnyItem, boolean canSupplyItems,
                                 boolean canReceiveItems, List<ItemEntry> items) {
    }

    public record ItemEntry(String identifier, int extractableCount, boolean acceptsMore) {
    }
}
