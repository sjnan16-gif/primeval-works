package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.item.DinoWhistleItem;
import com.primevalworks.world.work.DinoWhistleSettings;
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
        context.enqueueWork(() -> {
            ItemStack whistle = DinoWhistleItem.findInventoryWhistle(player, payload.inventorySlot);
            if (whistle.isEmpty()) return;
            DinoWhistleSettings.FieldMode mode = DinoWhistleSettings.FieldMode.byId(payload.mode);
            Identifier filterId = payload.itemFilter == null ? null : Identifier.tryParse(payload.itemFilter);
            String filter = mode == DinoWhistleSettings.FieldMode.COLLECT && filterId != null
                    && BuiltInRegistries.ITEM.get(filterId).isPresent() ? filterId.toString() : "";
            new DinoWhistleSettings(mode,
                    DinoWhistleSettings.Pattern.byId(payload.pattern), payload.range,
                    filter).write(whistle);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
