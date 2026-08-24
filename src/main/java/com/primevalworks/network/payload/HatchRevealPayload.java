package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.effect.DinosaurHatchReveal;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HatchRevealPayload(
        String species,
        String name,
        int quality,
        int mutationMask,
        int hueVariant
) implements CustomPacketPayload {
    public static final Type<HatchRevealPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "hatch_reveal")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HatchRevealPayload> STREAM_CODEC = StreamCodec.of(
            HatchRevealPayload::encode,
            HatchRevealPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, HatchRevealPayload payload) {
        buffer.writeUtf(payload.species, 48);
        buffer.writeUtf(payload.name, 96);
        buffer.writeVarInt(payload.quality);
        buffer.writeVarInt(payload.mutationMask);
        buffer.writeVarInt(payload.hueVariant + 8);
    }

    private static HatchRevealPayload decode(RegistryFriendlyByteBuf buffer) {
        return new HatchRevealPayload(
                buffer.readUtf(48), buffer.readUtf(96), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt() - 8
        );
    }

    public static void send(ServerPlayer player, FieldDodoEntity dinosaur) {
        if (!player.connection.hasChannel(TYPE)) return;
        PacketDistributor.sendToPlayer(player, new HatchRevealPayload(
                dinosaur.getSpecies().registryName(),
                dinosaur.getDisplayName().getString(),
                dinosaur.getGeneticQuality(),
                dinosaur.getMutationMask(),
                dinosaur.getHueVariant()
        ));
    }

    public static void handle(HatchRevealPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> DinosaurHatchReveal.show(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
