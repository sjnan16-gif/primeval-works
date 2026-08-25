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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

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
        OffspringGenome offspring = createOffspring(target, partner, target.getRandom());
        ItemStack egg = new ItemStack(DinosaurEggSize.forSpecies(target.getSpecies()).item());
        new DinosaurEggGenome(
                target.getSpecies(),
                DinosaurEggGenome.Origin.BRED,
                offspring.quality,
                offspring.mutationMask,
                offspring.hueVariant
        ).writeTo(egg);
        if (!player.addItem(egg)) {
            ItemEntity dropped = new ItemEntity(
                    (ServerLevel)target.level(), target.getX(), target.getY() + 0.5D, target.getZ(), egg);
            dropped.setDefaultPickUpDelay();
            target.level().addFreshEntity(dropped);
        }

        long cooldown = Math.max(0L, Math.round(BREEDING_COOLDOWN_TICKS
                * PrimevalTuning.server().breedingCooldown()));
        target.beginBreedingCooldown(cooldown);
        partner.beginBreedingCooldown(cooldown);
        target.feed(-8);
        partner.feed(-8);
        DinosaurOwnership.syncRecord(target);
        DinosaurOwnership.syncRecord(partner);
        ServerLevel level = (ServerLevel)target.level();
        showHearts(level, target, 12);
        showHearts(level, partner, 12);
        PrimevalAdvancements.awardBreed(player);
        player.sendOverlayMessage(Component.translatable(
                "message.primevalworks.breeding.success",
                Component.translatable("entity.primevalworks." + target.getSpecies().registryName()),
                offspring.quality
        ));
        return InteractionResult.SUCCESS;
    }

    public static OffspringGenome createOffspring(
            FieldDodoEntity first,
            FieldDodoEntity second,
            RandomSource random
    ) {
        int averageQuality = (first.getGeneticQuality() + second.getGeneticQuality()) / 2;
        int quality = Mth.clamp(averageQuality + 4 + random.nextInt(7), 0, 100);
        int mutationMask = DinosaurMutationRules.rollBred(
                first.getMutationMask(), second.getMutationMask(), random.nextFloat(), random.nextFloat());
        quality = Mth.clamp(quality + DinosaurMutationRules.qualityBonus(mutationMask, random.nextFloat()), 0, 100);
        int averageHue = Math.round((first.getHueVariant() + second.getHueVariant()) * 0.5F);
        int hueVariant = Mth.clamp(averageHue + random.nextInt(5) - 2, -8, 8);
        return new OffspringGenome(quality, mutationMask, hueVariant);
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

    private static void showHearts(ServerLevel level, FieldDodoEntity dinosaur, int count) {
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
