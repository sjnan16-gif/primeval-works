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

public record DinosaurWorkStatePayload(AssignDodoWorkPayload assignment, boolean enabled, int baseRadius)
        implements CustomPacketPayload {
    public static final Type<DinosaurWorkStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dinosaur_work_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DinosaurWorkStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.enabled);
                buffer.writeVarInt(payload.baseRadius);
                AssignDodoWorkPayload.encode(buffer, payload.assignment);
            },
            DinosaurWorkStatePayload::decode
    );

    private static DinosaurWorkStatePayload decode(RegistryFriendlyByteBuf buffer) {
        boolean enabled = buffer.readBoolean();
        int baseRadius = Mth.clamp(buffer.readVarInt(), 8, 128);
        return new DinosaurWorkStatePayload(AssignDodoWorkPayload.decode(buffer), enabled, baseRadius);
    }

    public static DinosaurWorkStatePayload from(FieldDodoEntity dinosaur) {
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
                dinosaur.shouldAvoidDanger()
        ), dinosaur.isWorkEnabled(), CommandTableBlock.baseRadius(dinosaur.level(), tablePos));
    }

    public static void handle(DinosaurWorkStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WorksitePlannerScreen.acceptWorkState(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
