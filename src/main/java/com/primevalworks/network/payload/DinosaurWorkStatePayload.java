package com.primevalworks.network.payload;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.client.screen.WorksitePlannerScreen;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DinosaurWorkStatePayload(
        AssignDodoWorkPayload assignment,
        boolean enabled,
        int baseRadius,
        boolean accepted,
        String message
)
        implements CustomPacketPayload {
    public static final Type<DinosaurWorkStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dinosaur_work_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DinosaurWorkStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.enabled);
                buffer.writeVarInt(payload.baseRadius);
                buffer.writeBoolean(payload.accepted);
                buffer.writeUtf(payload.message, 192);
                AssignDodoWorkPayload.encode(buffer, payload.assignment);
            },
            DinosaurWorkStatePayload::decode
    );

    private static DinosaurWorkStatePayload decode(RegistryFriendlyByteBuf buffer) {
        boolean enabled = buffer.readBoolean();
        int baseRadius = Mth.clamp(buffer.readVarInt(), 8, 128);
        boolean accepted = buffer.readBoolean();
        String message = buffer.readUtf(192);
        return new DinosaurWorkStatePayload(
                AssignDodoWorkPayload.decode(buffer), enabled, baseRadius, accepted, message);
    }

    public static DinosaurWorkStatePayload from(FieldDodoEntity dinosaur) {
        return from(dinosaur, 0L);
    }

    public static DinosaurWorkStatePayload from(FieldDodoEntity dinosaur, long requestId) {
        BlockPos tablePos = dinosaur.getCommandTablePos().orElse(dinosaur.blockPosition());
        return new DinosaurWorkStatePayload(new AssignDodoWorkPayload(
                dinosaur.getId(),
                dinosaur.getWorkJobIndex(),
                tablePos,
                dinosaur.getWorkSourcePositions(),
                dinosaur.getWorkWorkstationPositions(),
                dinosaur.getWorkDestinationPositions(),
                dinosaur.getWorkAreaEndPos(),
                dinosaur.getWorkFallbackPositions(),
                dinosaur.getWorkItemFilters(),
                dinosaur.getWorkFuelFilters(),
                dinosaur.getWorkBlockPriorities(),
                dinosaur.getExpeditionTier(),
                dinosaur.getWorkPriority(),
                dinosaur.getWorkBatchSize(),
                dinosaur.getWorkSchedule(),
                dinosaur.getWorkSourceReserve(),
                dinosaur.getWorkDestinationTarget(),
                dinosaur.getWorkRepeatMode(),
                dinosaur.getWorkRoutePolicy(),
                dinosaur.isExactItemMatch(),
                dinosaur.shouldAvoidDanger(),
                requestId
        ), dinosaur.isWorkEnabled(), CommandTableBlock.baseRadius(dinosaur.level(), tablePos), true, "");
    }

    public static DinosaurWorkStatePayload rejected(AssignDodoWorkPayload attempted, String message) {
        return new DinosaurWorkStatePayload(
                attempted,
                false,
                50,
                false,
                message == null ? "That work order could not be saved." : message
        );
    }

    public static void handle(DinosaurWorkStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WorksitePlannerScreen.acceptWorkState(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
