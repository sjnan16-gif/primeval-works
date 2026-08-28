package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.work.DinoWhistleSettings;
import com.primevalworks.world.ownership.DinosaurOwnership;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ConfigureDinoWhistlePayload(int inventorySlot, int mode, int pattern, int range, String itemFilter)
        implements CustomPacketPayload {
    public static final Type<ConfigureDinoWhistlePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "configure_dino_whistle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureDinoWhistlePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.inventorySlot);
                buffer.writeVarInt(payload.mode);
                buffer.writeVarInt(payload.pattern);
                buffer.writeVarInt(payload.range);
                buffer.writeUtf(payload.itemFilter, 160);
            }, buffer -> new ConfigureDinoWhistlePayload(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(160)));

    public static void handle(ConfigureDinoWhistlePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> apply(player, payload));
    }

    /** Applies one complete settings snapshot to the exact physical Whistle slot. */
    public static boolean apply(ServerPlayer player, ConfigureDinoWhistlePayload payload) {
        ItemStack whistle = DinoWhistleItem.findInventoryWhistle(player, payload.inventorySlot);
        if (whistle.isEmpty()) return false;
        DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.byId(payload.mode);
        Identifier filterId = payload.itemFilter == null ? null : Identifier.tryParse(payload.itemFilter);
        String filter = mode == DinoWhistleSettings.FieldMode.COLLECT && filterId != null
                && BuiltInRegistries.ITEM.get(filterId).isPresent() ? filterId.toString() : "";
        DinoWhistleSettings updated = new DinoWhistleSettings(mode,
                DinoWhistleSettings.Pattern.byId(payload.pattern), payload.range, filter);
        if (updated.equals(DinoWhistleSettings.read(whistle))) return true;

        // Replacing the slot guarantees component sync in both inventory and container views.
        ItemStack updatedWhistle = whistle.copy();
        updated.write(updatedWhistle);
        player.getInventory().setItem(payload.inventorySlot, updatedWhistle);
        if (updated.mode().isPassive()) {
            DinosaurOwnership.loadedFollowers(player).forEach(
                    dinosaur -> dinosaur.updatePassiveFieldSettings(updated));
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return true;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
