package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.WorksitePlannerScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record CraftingCataloguePayload(List<String> itemIdentifiers) implements CustomPacketPayload {
    public static final Type<CraftingCataloguePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "crafting_catalogue")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingCataloguePayload> STREAM_CODEC = StreamCodec.of(
            CraftingCataloguePayload::encode,
            CraftingCataloguePayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, CraftingCataloguePayload payload) {
        int count = Math.min(4096, payload.itemIdentifiers.size());
        buffer.writeVarInt(count);
        payload.itemIdentifiers.stream().limit(count).forEach(value -> buffer.writeUtf(value, 128));
    }

    private static CraftingCataloguePayload decode(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 4096) throw new IllegalArgumentException("Invalid crafting catalogue size: " + count);
        List<String> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) values.add(buffer.readUtf(128));
        return new CraftingCataloguePayload(List.copyOf(values));
    }

    public static void handle(CraftingCataloguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHandler.accept(payload));
    }

    private static final class ClientHandler {
        private static void accept(CraftingCataloguePayload payload) {
            WorksitePlannerScreen.acceptCraftingCatalogue(payload.itemIdentifiers);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
