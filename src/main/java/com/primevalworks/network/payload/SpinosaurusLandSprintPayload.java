package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpinosaurusLandSprintPayload(int entityId, boolean sprinting) implements CustomPacketPayload {
    public static final Type<SpinosaurusLandSprintPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "spinosaurus_land_sprint")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SpinosaurusLandSprintPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SpinosaurusLandSprintPayload::entityId,
                    ByteBufCodecs.BOOL,
                    SpinosaurusLandSprintPayload::sprinting,
                    SpinosaurusLandSprintPayload::new
            );

    public static void handle(SpinosaurusLandSprintPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.getVehicle() instanceof FieldDodoEntity dinosaur)
                || dinosaur.getId() != payload.entityId()
                || dinosaur.getSpecies() != DinosaurSpecies.SPINOSAURUS) {
            return;
        }
        dinosaur.setSpinosaurusLandSprinting(payload.sprinting());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
