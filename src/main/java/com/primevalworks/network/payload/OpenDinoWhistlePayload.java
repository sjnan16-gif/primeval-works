package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.DinoWhistleScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenDinoWhistlePayload() implements CustomPacketPayload {
    public static final Type<OpenDinoWhistlePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "open_dino_whistle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDinoWhistlePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {}, buffer -> new OpenDinoWhistlePayload());

    public static void handle(OpenDinoWhistlePayload payload, IPayloadContext context) {
        context.enqueueWork(DinoWhistleScreen::open);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
