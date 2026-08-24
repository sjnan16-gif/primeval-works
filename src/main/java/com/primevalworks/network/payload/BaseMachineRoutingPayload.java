package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.WorksitePlannerScreen;
import com.primevalworks.world.block.entity.FoodBoxBlockEntity;
import com.primevalworks.world.block.entity.ProcessorBlockEntity;
import com.primevalworks.world.inventory.AutomationConfigurableContainer;
import com.primevalworks.world.work.BaseInventoryIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record BaseMachineRoutingPayload(BlockPos tablePos, BlockPos machinePos, String machineName,
                                        List<SlotInfo> slots) implements CustomPacketPayload {
    private static final int MAX_SLOTS = 64;
    public static final Type<BaseMachineRoutingPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "base_machine_routing")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseMachineRoutingPayload> STREAM_CODEC = StreamCodec.of(
            BaseMachineRoutingPayload::encode,
            BaseMachineRoutingPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, BaseMachineRoutingPayload payload) {
        buffer.writeLong(payload.tablePos.asLong());
        buffer.writeLong(payload.machinePos.asLong());
        buffer.writeUtf(payload.machineName, 96);
        int count = Math.min(MAX_SLOTS, payload.slots.size());
        buffer.writeVarInt(count);
        for (SlotInfo slot : payload.slots.subList(0, count)) {
            buffer.writeVarInt(slot.index);
            buffer.writeUtf(slot.role, 32);
            buffer.writeUtf(slot.itemIdentifier, 128);
            buffer.writeVarInt(Math.max(0, slot.count));
            buffer.writeBoolean(slot.canInsert);
            buffer.writeBoolean(slot.canExtract);
            buffer.writeBoolean(slot.insertEnabled);
            buffer.writeBoolean(slot.extractEnabled);
            buffer.writeBoolean(slot.configurable);
        }
    }

    private static BaseMachineRoutingPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos tablePos = BlockPos.of(buffer.readLong());
        BlockPos machinePos = BlockPos.of(buffer.readLong());
        String name = buffer.readUtf(96);
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_SLOTS) throw new IllegalArgumentException("Invalid machine slot count: " + count);
        List<SlotInfo> slots = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            slots.add(new SlotInfo(buffer.readVarInt(), buffer.readUtf(32), buffer.readUtf(128),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean()));
        }
        return new BaseMachineRoutingPayload(tablePos, machinePos, name, List.copyOf(slots));
    }

    public static void send(ServerPlayer player, BlockPos tablePos, BlockPos machinePos, Container container) {
        List<SlotInfo> slots = new ArrayList<>(Math.min(MAX_SLOTS, container.getContainerSize()));
        boolean configurable = container instanceof AutomationConfigurableContainer;
        for (int slot = 0; slot < container.getContainerSize() && slots.size() < MAX_SLOTS; slot++) {
            ItemStack stack = container.getItem(slot);
            boolean canInsert = canEverInsert(container, slot, stack);
            boolean canExtract = canEverExtract(container, slot, stack);
            boolean insertEnabled = !(container instanceof AutomationConfigurableContainer automation)
                    || automation.allowsAutomationInsert(slot);
            boolean extractEnabled = !(container instanceof AutomationConfigurableContainer automation)
                    || automation.allowsAutomationExtract(slot);
            String identifier = stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            slots.add(new SlotInfo(slot, role(container, slot), identifier, stack.getCount(), canInsert, canExtract,
                    canInsert && insertEnabled, canExtract && extractEnabled, configurable && (canInsert || canExtract)));
        }
        String machineName = player.level().getBlockState(machinePos).getBlock().getName().getString();
        PacketDistributor.sendToPlayer(player, new BaseMachineRoutingPayload(
                tablePos.immutable(), machinePos.immutable(), machineName, List.copyOf(slots)));
    }

    private static boolean canEverInsert(Container container, int slot, ItemStack present) {
        if (container instanceof ProcessorBlockEntity) return slot != ProcessorBlockEntity.OUTPUT_SLOT;
        if (container instanceof AbstractFurnaceBlockEntity) return slot == 0 || slot == 1;
        if (container instanceof FoodBoxBlockEntity) return true;
        if (!present.isEmpty()) return BaseInventoryIndex.canInsert(container, slot, present);
        return true;
    }

    private static boolean canEverExtract(Container container, int slot, ItemStack present) {
        if (container instanceof ProcessorBlockEntity) return slot == ProcessorBlockEntity.OUTPUT_SLOT;
        if (container instanceof AbstractFurnaceBlockEntity) return slot == 2;
        if (container instanceof FoodBoxBlockEntity) return false;
        return present.isEmpty() || BaseInventoryIndex.canExtract(container, slot, present);
    }

    private static String role(Container container, int slot) {
        if (container instanceof ProcessorBlockEntity) {
            return switch (slot) {
                case ProcessorBlockEntity.INPUT_SLOT -> "INPUT";
                case ProcessorBlockEntity.FUEL_SLOT -> "FUEL";
                case ProcessorBlockEntity.CATALYST_SLOT -> "CATALYST";
                case ProcessorBlockEntity.OUTPUT_SLOT -> "OUTPUT";
                default -> "SLOT " + (slot + 1);
            };
        }
        if (container instanceof AbstractFurnaceBlockEntity) {
            return switch (slot) {
                case 0 -> "INPUT";
                case 1 -> "FUEL";
                case 2 -> "OUTPUT";
                default -> "SLOT " + (slot + 1);
            };
        }
        if (container instanceof FoodBoxBlockEntity) return "FOOD " + (slot + 1);
        return "STORAGE " + (slot + 1);
    }

    public static void handle(BaseMachineRoutingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WorksitePlannerScreen.acceptMachineRouting(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record SlotInfo(int index, String role, String itemIdentifier, int count, boolean canInsert,
                           boolean canExtract, boolean insertEnabled, boolean extractEnabled, boolean configurable) {
    }
}
