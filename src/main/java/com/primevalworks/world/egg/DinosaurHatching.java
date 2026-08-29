package com.primevalworks.world.egg;

import com.primevalworks.registry.ModBlocks;
import com.primevalworks.registry.ModEntities;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.network.payload.HatchRevealPayload;
import com.primevalworks.world.progression.PrimevalAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class DinosaurHatching {
    public static final int ACTIVE_DINOSAUR_LIMIT = 7;

    private DinosaurHatching() {
    }

    public static HatchResult hatchWildEgg(ServerPlayer player, DinosaurEggSize size) {
        return hatchForPlayer(player, Genome.wild(size.randomSpecies(player.level().getRandom())));
    }

    public static HatchResult hatchForPlayer(ServerPlayer player, Genome genome) {
        if (!genome.species().isPlayable()) {
            return HatchResult.failure(Component.literal("That dinosaur is not part of this build."));
        }
        CommandTableBlock.ClaimedTable table = CommandTableBlock.getClaimedTable(player).orElse(null);
        if (table == null) {
            FieldDodoEntity dinosaur = createHatchling(player.level(), player.blockPosition(), player.getUUID(), genome);
            if (dinosaur == null) {
                return HatchResult.failure(Component.translatable("message.primevalworks.egg.no_room"));
            }
            DinosaurOwnership.register(player, dinosaur);
            PrimevalAdvancements.awardHatch(player, dinosaur, false);
            HatchRevealPayload.send(player, dinosaur);
            Component success = Component.literal(
                    dinosaur.getDisplayName().getString() + " is yours. It will wait for your first Command Table."
            );
            player.sendOverlayMessage(success);
            return HatchResult.success(dinosaur, success);
        }
        return hatchAtTable(table.level(), table.pos(), player.getUUID(), genome);
    }

    public static HatchResult hatchAtTable(ServerLevel level, BlockPos tablePos, UUID owner, Genome genome) {
        if (!genome.species().isPlayable()) {
            return HatchResult.failure(Component.literal("That dinosaur is not part of this build."));
        }
        if (!level.getBlockState(tablePos).is(ModBlocks.COMMAND_TABLE.get())) {
            return HatchResult.failure(Component.translatable("message.primevalworks.egg.base_missing"));
        }
        ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
        if (ownerPlayer == null) {
            return HatchResult.failure(Component.literal(
                    "Incubation is complete. The owner needs to return before hatching can finish."
            ));
        }
        FieldDodoEntity dinosaur = createHatchling(level, tablePos, owner, genome);
        if (dinosaur == null) {
            return HatchResult.failure(Component.translatable("message.primevalworks.egg.no_room"));
        }

        boolean active = DinosaurOwnership.addToActiveIfRoom(ownerPlayer, dinosaur, tablePos);
        boolean saved = DinosaurOwnership.records(ownerPlayer).stream()
                .anyMatch(record -> record.id().equals(dinosaur.getUUID()));
        if (!saved) {
            dinosaur.unlinkFromCommandTable();
            dinosaur.discard();
            return HatchResult.failure(Component.literal(
                    "Your dinosaur depot is full. Make room before hatching this egg."
            ));
        }
        if (!active) {
            dinosaur.unlinkFromCommandTable();
            dinosaur.discard();
        }

        if (level.getBlockEntity(tablePos) instanceof CommandTableBlockEntity table) {
            table.addInsight(genome.incubated() ? 2 : 1);
        }

        level.sendParticles(
                genome.incubated() ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.POOF,
                dinosaur.getX(), dinosaur.getY() + dinosaur.getBbHeight() * 0.55D, dinosaur.getZ(),
                genome.incubated() ? 20 : 14,
                dinosaur.getBbWidth() * 0.28D, dinosaur.getBbHeight() * 0.18D, dinosaur.getBbWidth() * 0.28D,
                0.025D
        );
        Component success = active
                ? Component.translatable("message.primevalworks.egg.joined_base", dinosaur.getDisplayName())
                : Component.literal(dinosaur.getDisplayName().getString() + " was safely moved into your dinosaur depot.");
        ownerPlayer.sendOverlayMessage(success);
        PrimevalAdvancements.awardHatch(ownerPlayer, dinosaur, genome.incubated());
        HatchRevealPayload.send(ownerPlayer, dinosaur);
        return HatchResult.success(dinosaur, success);
    }

    private static @Nullable FieldDodoEntity createHatchling(
            ServerLevel level, BlockPos center, UUID owner, Genome genome
    ) {
        FieldDodoEntity dinosaur = ModEntities.typeFor(genome.species()).create(level, EntitySpawnReason.BREEDING);
        if (dinosaur == null) return null;
        BlockPos spawnPos = findSpawnPosition(level, center, dinosaur);
        if (spawnPos == null) {
            dinosaur.discard();
            return null;
        }
        dinosaur.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        dinosaur.setYRot(level.getRandom().nextFloat() * 360.0F);
        dinosaur.setDinosaurOwner(owner);
        if (genome.hasDefinedGenetics()) {
            dinosaur.applyIncubatedGenetics(genome.quality(), genome.mutationMask(), genome.hueVariant());
        } else {
            dinosaur.initializeWildHatch();
        }
        if (!level.addFreshEntity(dinosaur)) {
            dinosaur.discard();
            return null;
        }
        return dinosaur;
    }

    private static @Nullable BlockPos findSpawnPosition(ServerLevel level, BlockPos tablePos, FieldDodoEntity dinosaur) {
        for (int radius = 2; radius <= 10; radius++) {
            for (int edge = -radius; edge <= radius; edge++) {
                BlockPos found = validSpawn(level, tablePos.offset(edge, 0, -radius), dinosaur);
                if (found != null) return found;
                found = validSpawn(level, tablePos.offset(edge, 0, radius), dinosaur);
                if (found != null) return found;
                found = validSpawn(level, tablePos.offset(-radius, 0, edge), dinosaur);
                if (found != null) return found;
                found = validSpawn(level, tablePos.offset(radius, 0, edge), dinosaur);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static @Nullable BlockPos validSpawn(ServerLevel level, BlockPos horizontal, FieldDodoEntity dinosaur) {
        for (int vertical = 3; vertical >= -3; vertical--) {
            BlockPos candidate = horizontal.offset(0, vertical, 0);
            if (!level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), net.minecraft.core.Direction.UP)) {
                continue;
            }
            dinosaur.setPos(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
            if (level.noCollision(dinosaur)) {
                return candidate.immutable();
            }
        }
        return null;
    }

    public record Genome(DinosaurSpecies species, Origin origin, int quality, int mutationMask, int hueVariant) {
        public Genome {
            quality = Mth.clamp(quality, 0, 100);
            mutationMask &= FieldDodoEntity.MUTATION_HUGE | FieldDodoEntity.MUTATION_ALBINO;
            hueVariant = Mth.clamp(hueVariant, -8, 8);
        }

        public static Genome wild(DinosaurSpecies species) {
            return new Genome(species, Origin.WILD, 0, 0, 0);
        }

        public static Genome bred(DinosaurSpecies species, int quality, int mutationMask, int hueVariant) {
            return new Genome(species, Origin.BRED, quality, mutationMask, hueVariant);
        }

        public static Genome incubated(DinosaurSpecies species, int quality, int mutationMask, int hueVariant) {
            return new Genome(species, Origin.INCUBATED, quality, mutationMask, hueVariant);
        }

        public boolean incubated() {
            return origin == Origin.INCUBATED;
        }

        public boolean hasDefinedGenetics() {
            return origin != Origin.WILD;
        }
    }

    public enum Origin {
        WILD,
        BRED,
        INCUBATED
    }

    public record HatchResult(boolean success, @Nullable FieldDodoEntity dinosaur, Component message) {
        public static HatchResult success(FieldDodoEntity dinosaur, Component message) {
            return new HatchResult(true, dinosaur, message);
        }

        public static HatchResult failure(Component message) {
            return new HatchResult(false, null, message);
        }
    }
}
