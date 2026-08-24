package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FeedDodoPayload(int entityId, int inventorySlot) implements CustomPacketPayload {
    public static final Type<FeedDodoPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "feed_dodo")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FeedDodoPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            FeedDodoPayload::entityId,
            ByteBufCodecs.VAR_INT,
            FeedDodoPayload::inventorySlot,
            FeedDodoPayload::new
    );

    public static void handle(FeedDodoPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level().getEntity(payload.entityId()) instanceof FieldDodoEntity dodo)
                || !dodo.isAlive()
                || player.distanceToSqr(dodo) > 64.0D) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (payload.inventorySlot() < 0 || payload.inventorySlot() >= inventory.getNonEquipmentItems().size()) {
            return;
        }

        ItemStack food = inventory.getItem(payload.inventorySlot());
        if (!dodo.canEat(food)) {
            return;
        }

        dodo.eat(food, 18);
        if (!player.getAbilities().instabuild) {
            food.shrink(1);
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
