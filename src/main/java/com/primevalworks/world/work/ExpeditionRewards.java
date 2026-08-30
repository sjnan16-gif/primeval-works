package com.primevalworks.world.work;

import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.entity.SpinosaurusTrophyRules;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ExpeditionRewards {
    private static final List<Tier> TIERS = List.of(
            new Tier("Safe Forage", 1, List.of(
                    reward(() -> ModItems.BERRIES.get(), 3, 6, 7),
                    reward(() -> ModItems.FOSSIL_FRAGMENT.get(), 1, 1, 2),
                    reward(() -> Items.WHEAT_SEEDS, 3, 7, 5),
                    reward(() -> Items.FLINT, 1, 3, 3)
            )),
            new Tier("Ridge Trail", 2, List.of(
                    reward(() -> ModItems.SULFUR.get(), 1, 3, 6),
                    reward(() -> ModItems.FOSSIL_FRAGMENT.get(), 1, 2, 3),
                    reward(() -> ModItems.HARDWOOD.get(), 2, 4, 4),
                    reward(() -> Items.COAL, 2, 5, 3)
            )),
            new Tier("Deep Wilds", 2, List.of(
                    reward(() -> ModItems.SILK.get(), 2, 4, 6),
                    reward(() -> ModItems.SULFUR.get(), 2, 4, 5),
                    reward(() -> ModItems.FOSSIL_FRAGMENT.get(), 1, 2, 4),
                    reward(() -> ModItems.NESTING_TREAT.get(), 1, 1, 2),
                    reward(() -> ModItems.CORE.get(), 1, 2, 4),
                    reward(() -> ModItems.RAW_ANCIENT_METAL_INGOT.get(), 1, 1, 1)
            )),
            new Tier("Predator Run", 3, List.of(
                    reward(() -> ModItems.TYRANNOSAURUS_TOOTH.get(), 1, 2, 3),
                    reward(() -> ModItems.PTERANODON_WING_FRAGMENT.get(), 1, 2, 3),
                    reward(() -> ModItems.SILK.get(), 3, 6, 4),
                    reward(() -> ModItems.SULFUR.get(), 3, 6, 4),
                    reward(() -> ModItems.FOSSIL_FRAGMENT.get(), 1, 2, 2),
                    reward(() -> ModItems.NESTING_TREAT.get(), 1, 2, 2),
                    reward(() -> ModItems.CORE.get(), 1, 3, 5),
                    reward(() -> ModItems.RAW_ANCIENT_METAL_INGOT.get(), 1, 2, 3)
            )),
            new Tier("Primordial Frontier", 3, List.of(
                    reward(() -> ModItems.RAW_ANCIENT_METAL_INGOT.get(), 2, 3, 7),
                    reward(() -> ModItems.MAGIC_SHARD_FRAGMENT.get(), 1, 1, 3),
                    reward(() -> ModItems.COMPRESSED_CORE.get(), 1, 1, 1),
                    reward(() -> ModItems.CORE.get(), 2, 4, 7),
                    reward(() -> ModItems.PTERANODON_WING_FRAGMENT.get(), 1, 2, 3),
                    reward(() -> ModItems.TYRANNOSAURUS_TOOTH.get(), 1, 2, 3),
                    reward(() -> ModItems.SILK.get(), 3, 6, 3),
                    reward(() -> ModItems.FOSSIL_FRAGMENT.get(), 1, 3, 2),
                    reward(() -> ModItems.NESTING_TREAT.get(), 1, 2, 2),
                    rareReward(() -> ModItems.SPINOSAURUS_HEAD.get(), 1, 1,
                            SpinosaurusTrophyRules.frontierExpeditionChance())
            ))
    );

    private ExpeditionRewards() {
    }

    public static Tier tier(int index) {
        return TIERS.get(Math.max(0, Math.min(TIERS.size() - 1, index)));
    }

    public static List<ItemStack> roll(int tier, int gatheringStars, float tableRewardMultiplier,
                                       RandomSource random) {
        Tier definition = tier(tier);
        int rolls = Math.max(0, Math.round(definition.rolls()
                * (float)PrimevalTuning.server().expeditionRewards()));
        float bonusChance = Math.min(0.55F,
                Math.max(0, gatheringStars) * 0.08F + Math.max(0.0F, tableRewardMultiplier - 1.0F));
        if (rolls > 0 && random.nextFloat() < bonusChance) rolls++;

        Map<Item, ItemStack> combined = new LinkedHashMap<>();
        for (int roll = 0; roll < rolls; roll++) {
            Reward reward = choose(definition.rewards(), random);
            add(combined, reward, random);
        }
        for (Reward reward : definition.rewards()) {
            if (reward.rareChance() > 0.0F && random.nextFloat() < reward.rareChance()) {
                add(combined, reward, random);
            }
        }
        return new ArrayList<>(combined.values());
    }

    public static String shortPoolDescription(int tier) {
        return switch (Math.max(0, Math.min(4, tier))) {
            case 0 -> "Ember Berries, feathers, seeds, fossil fragments";
            case 1 -> "Sulfur, feathers, fossils, hardwood, coal";
            case 2 -> "Silk, sulfur, nesting treats, cores, rare metal";
            case 3 -> "Teeth, wings, nesting treats, cores, ancient metal";
            default -> "Ancient ore, cores, rare compressed cores, trophies, nesting treats, rare Spino heads";
        };
    }

    private static Reward choose(List<Reward> rewards, RandomSource random) {
        int totalWeight = rewards.stream().mapToInt(Reward::weight).sum();
        int value = random.nextInt(totalWeight);
        for (Reward reward : rewards) {
            if (reward.weight() <= 0) continue;
            value -= reward.weight();
            if (value < 0) return reward;
        }
        return rewards.stream()
                .filter(reward -> reward.weight() > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expedition has no weighted rewards"));
    }

    private static void add(Map<Item, ItemStack> combined, Reward reward, RandomSource random) {
        Item item = reward.item().get();
        int count = reward.minimum() + random.nextInt(reward.maximum() - reward.minimum() + 1);
        combined.compute(item, (ignored, existing) -> {
            if (existing == null) return new ItemStack(item, count);
            existing.grow(count);
            return existing;
        });
    }

    private static Reward reward(Supplier<Item> item, int minimum, int maximum, int weight) {
        return new Reward(item, minimum, maximum, weight, 0.0F);
    }

    private static Reward rareReward(Supplier<Item> item, int minimum, int maximum, float chance) {
        return new Reward(item, minimum, maximum, 0, chance);
    }

    public record Tier(String name, int rolls, List<Reward> rewards) {
    }

    public record Reward(Supplier<Item> item, int minimum, int maximum, int weight, float rareChance) {
    }
}
