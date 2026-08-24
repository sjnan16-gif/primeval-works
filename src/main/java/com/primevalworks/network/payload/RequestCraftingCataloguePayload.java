package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashSet;

public record RequestCraftingCataloguePayload(BlockPos commandTablePos) implements CustomPacketPayload {
    public static final Type<RequestCraftingCataloguePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "request_crafting_catalogue")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCraftingCataloguePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeLong(payload.commandTablePos.asLong()),
            buffer -> new RequestCraftingCataloguePayload(BlockPos.of(buffer.readLong()))
    );

    public static void handle(RequestCraftingCataloguePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.commandTablePos.getCenter()) > 4096.0D
                || !player.level().getBlockState(payload.commandTablePos).is(ModBlocks.COMMAND_TABLE.get())) {
            return;
        }
        var outputs = new LinkedHashSet<String>();
        var displayContext = SlotDisplayContext.fromLevel(player.level());
        for (var holder : player.level().getServer().getRecipeManager().recipeMap().byType(RecipeType.CRAFTING)) {
            holder.value().display().stream()
                    .flatMap(display -> display.result().resolveForStacks(displayContext).stream())
                    .filter(stack -> !stack.isEmpty())
                    .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                    .limit(4096)
                    .forEach(outputs::add);
            if (outputs.size() >= 4096) break;
        }
        PacketDistributor.sendToPlayer(player, new CraftingCataloguePayload(outputs.stream().limit(4096).toList()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
