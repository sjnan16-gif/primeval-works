package com.primevalworks.world.breeding;

import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.egg.DinosaurEggGenome;
import com.primevalworks.world.egg.DinosaurEggSize;
import com.primevalworks.world.entity.DinosaurMutationRules;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.progression.PrimevalAdvancements;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class DinosaurBreeding {
    public static final long BREEDING_COOLDOWN_TICKS = 12_000L;
    public static final int MINIMUM_HUNGER = 50;
    public static final int MINIMUM_MOOD = 55;
    private static final double PARTNER_RANGE = 16.0D;

    private DinosaurBreeding() {
    }

    public static InteractionResult useTreat(ServerPlayer player, FieldDodoEntity target, ItemStack held) {
        Component blocked = breedingBlockReason(player, target);
        if (blocked != null) {
            player.sendOverlayMessage(blocked);
            return InteractionResult.FAIL;
        }
        if (target.isBreedingPrimed()) {
            player.sendOverlayMessage(Component.translatable(
                    "message.primevalworks.breeding.already_primed", target.getDisplayName()));
            return InteractionResult.FAIL;
        }

        List<FieldDodoEntity> waiting = target.level().getEntitiesOfClass(
                FieldDodoEntity.class,
                target.getBoundingBox().inflate(PARTNER_RANGE),
                candidate -> candidate != target
                        && candidate.isAlive()
                        && candidate.isOwnedBy(player.getUUID())
                        && candidate.isBreedingPrimed()
        );
        FieldDodoEntity partner = waiting.stream()
                .filter(candidate -> candidate.getSpecies() == target.getSpecies())
                .min(Comparator.comparingDouble(target::distanceToSqr))
                .orElse(null);
        if (partner == null && !waiting.isEmpty()) {
            FieldDodoEntity mismatch = waiting.stream()
                    .min(Comparator.comparingDouble(target::distanceToSqr)).orElseThrow();
            player.sendOverlayMessage(Component.translatable(
                    "message.primevalworks.breeding.species_mismatch",
                    mismatch.getDisplayName(), target.getDisplayName()));
            return InteractionResult.FAIL;
        }
        if (partner == null) {
            held.consume(1, player);
            target.setBreedingPrimed(true);
            DinosaurOwnership.syncRecord(target);
            showHearts((ServerLevel)target.level(), target, 7);
            player.sendOverlayMessage(Component.translatable(
                    "message.primevalworks.breeding.waiting", target.getDisplayName()));
            return InteractionResult.SUCCESS;
        }

        Component partnerBlocked = breedingBlockReason(player, partner);
        if (partnerBlocked != null) {
            player.sendOverlayMessage(partnerBlocked);
            return InteractionResult.FAIL;
        }

        held.consume(1, player);
        target.startBreedingCourtship(partner, player);
        partner.startBreedingCourtship(target, player);
        DinosaurOwnership.syncRecord(target);
        DinosaurOwnership.syncRecord(partner);
        ServerLevel level = (ServerLevel)target.level();
        showHearts(level, target, 5);
        showHearts(level, partner, 5);
        player.sendOverlayMessage(Component.translatable(
                "message.primevalworks.breeding.courting"));
        return InteractionResult.SUCCESS;
    }

    public static void completeCourtship(
            @Nullable ServerPlayer player,
            UUID ownerId,
            FieldDodoEntity first,
            FieldDodoEntity second
    ) {
        if (!first.isAlive() || !second.isAlive()
                || first.getSpecies() != second.getSpecies()
                || !first.isOwnedBy(ownerId)
                || !second.isOwnedBy(ownerId)
                || !first.isBreedingWith(second.getUUID())
                || !second.isBreedingWith(first.getUUID())) return;
        OffspringGenome offspring = createOffspring(first, second, first.getRandom());
        ItemStack egg = new ItemStack(DinosaurEggSize.forSpecies(first.getSpecies()).item());
        new DinosaurEggGenome(
                first.getSpecies(),
                DinosaurEggGenome.Origin.BRED,
                offspring.quality,
                offspring.mutationMask,
                offspring.hueVariant
        ).writeTo(egg);
        ServerLevel level = (ServerLevel)first.level();
        if (!placeBredEgg(level, first, second, egg)) {
            ItemEntity dropped = new ItemEntity(
                    level,
                    (first.getX() + second.getX()) * 0.5D,
                    Math.min(first.getY(), second.getY()) + 0.2D,
                    (first.getZ() + second.getZ()) * 0.5D,
                    egg
            );
            dropped.setPickUpDelay(80);
            level.addFreshEntity(dropped);
        }
        long cooldown = Math.max(0L, Math.round(BREEDING_COOLDOWN_TICKS
                * PrimevalTuning.server().breedingCooldown()));
        first.beginBreedingCooldown(cooldown);
        second.beginBreedingCooldown(cooldown);
        first.feed(-8);
        second.feed(-8);
        DinosaurOwnership.syncRecord(first);
        DinosaurOwnership.syncRecord(second);
        showHearts(level, first, 12);
        showHearts(level, second, 12);
        if (player != null) {
            PrimevalAdvancements.awardBreed(player);
            player.sendOverlayMessage(Component.translatable(
                    "message.primevalworks.breeding.success",
                    Component.translatable("entity.primevalworks." + first.getSpecies().registryName()),
                    offspring.quality
            ));
        }
    }

    public static OffspringGenome createOffspring(
            FieldDodoEntity first,
            FieldDodoEntity second,
            RandomSource random
    ) {
        int quality = DinosaurBreedingRules.inheritedQuality(
                first.getGeneticQuality(), second.getGeneticQuality(), random.nextInt(7));
        int mutationMask = DinosaurMutationRules.rollBred(
                new DinosaurMutationRules.ParentGenetics(
                        first.getMutationMask(), first.getDinosaurLevel(), first.getGeneticQuality()),
                new DinosaurMutationRules.ParentGenetics(
                        second.getMutationMask(), second.getDinosaurLevel(), second.getGeneticQuality()),
                new DinosaurMutationRules.TraitRolls(random.nextFloat(), random.nextFloat(), random.nextFloat()),
                new DinosaurMutationRules.TraitRolls(random.nextFloat(), random.nextFloat(), random.nextFloat())
        );
        quality = Mth.clamp(quality + DinosaurMutationRules.qualityBonus(mutationMask, random.nextFloat()), 0, 100);
        int averageHue = Math.round((first.getHueVariant() + second.getHueVariant()) * 0.5F);
        int hueVariant = Mth.clamp(averageHue + random.nextInt(5) - 2, -8, 8);
        return new OffspringGenome(quality, mutationMask, hueVariant);
    }

    private static boolean placeBredEgg(
            ServerLevel level,
            FieldDodoEntity first,
            FieldDodoEntity second,
            ItemStack egg
    ) {
        DinosaurEggSize size = DinosaurEggSize.fromItem(egg).orElse(null);
        if (size == null) return false;
        BlockState eggState = size.block().defaultBlockState();
        BlockPos center = BlockPos.containing(
                (first.getX() + second.getX()) * 0.5D,
                Math.min(first.getY(), second.getY()),
                (first.getZ() + second.getZ()) * 0.5D
        );
        for (int radius = 0; radius <= 4; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;
                    for (int y : new int[]{0, 1, -1, 2, -2}) {
                        BlockPos candidate = center.offset(x, y, z);
                        if (!validEggPosition(level, candidate, eggState)) continue;
                        BlockState replaced = level.getBlockState(candidate);
                        if (!level.setBlock(candidate, eggState, 3)) continue;
                        if (level.getBlockEntity(candidate)
                                instanceof com.primevalworks.world.block.entity.DinosaurEggBlockEntity blockEntity
                                && blockEntity.setGeneticEgg(egg)) {
                            return true;
                        }
                        level.setBlock(candidate, replaced, 3);
                    }
                }
            }
        }
        return false;
    }

    private static boolean validEggPosition(ServerLevel level, BlockPos pos, BlockState eggState) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        BlockState existing = level.getBlockState(pos);
        if (!existing.canBeReplaced() || !existing.getFluidState().isEmpty()) return false;
        BlockPos supportPos = pos.below();
        if (!level.getBlockState(supportPos)
                .isFaceSturdy(level, supportPos, Direction.UP, SupportType.CENTER)) return false;
        if (!eggState.canSurvive(level, pos)) return false;
        return level.getEntities((net.minecraft.world.entity.Entity)null,
                new AABB(pos), entity -> entity.isAlive()).isEmpty();
    }

    private static Component breedingBlockReason(ServerPlayer player, FieldDodoEntity dinosaur) {
        if (!dinosaur.isOwnedBy(player.getUUID())) {
            return Component.translatable("message.primevalworks.breeding.not_owner");
        }
        if (dinosaur.isOnExpedition() || dinosaur.isIncapacitated() || !dinosaur.isAlive()) {
            return Component.translatable(
                    "message.primevalworks.breeding.unavailable", dinosaur.getDisplayName());
        }
        if (dinosaur.isVehicle() || dinosaur.getTarget() != null || dinosaur.isDinosaurSleeping()) {
            return Component.translatable(
                    "message.primevalworks.breeding.busy", dinosaur.getDisplayName());
        }
        if (dinosaur.getBreedingCooldownRemaining() > 0L) {
            long seconds = (dinosaur.getBreedingCooldownRemaining() + 19L) / 20L;
            return Component.translatable(
                    "message.primevalworks.breeding.cooldown", dinosaur.getDisplayName(), seconds / 60L, seconds % 60L);
        }
        if (dinosaur.getHunger() < MINIMUM_HUNGER) {
            return Component.translatable(
                    "message.primevalworks.breeding.hungry", dinosaur.getDisplayName(), MINIMUM_HUNGER);
        }
        if (dinosaur.getMood() < MINIMUM_MOOD) {
            return Component.translatable(
                    "message.primevalworks.breeding.mood", dinosaur.getDisplayName(), MINIMUM_MOOD);
        }
        return null;
    }

    public static void showHearts(ServerLevel level, FieldDodoEntity dinosaur, int count) {
        level.sendParticles(
                ParticleTypes.HEART,
                dinosaur.getX(), dinosaur.getY() + dinosaur.getBbHeight() * 0.72D, dinosaur.getZ(),
                count,
                dinosaur.getBbWidth() * 0.26D, dinosaur.getBbHeight() * 0.12D,
                dinosaur.getBbWidth() * 0.26D, 0.025D
        );
    }

    public record OffspringGenome(int quality, int mutationMask, int hueVariant) {
    }
}
