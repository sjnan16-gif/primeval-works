package com.primevalworks.world.work;

import com.primevalworks.registry.ModItems;
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
                    reward(() -> ModItems.DODO_FEATHER.get(), 1, 3, 5),
                    reward(() -> ModItems.FOSSIL_FRAGMENT.get(), 1, 1, 2),
                    reward(() -> Items.WHEAT_SEEDS, 3, 7, 5),
                    reward(() -> Items.FLINT, 1, 3, 3)
            )),
            new Tier("Ridge Trail", 2, List.of(
                    reward(() -> ModItems.SULFUR.get(), 1, 3, 6),
                    reward(() -> ModItems.DODO_FEATHER.get(), 2, 5, 4),
                    reward(() -> ModItems.FOSSIL_FRAGMENT.get(), 1, 2, 3),
                    reward(() -> ModItems.HARDWOOD.get(), 2, 4, 4),
                    reward(() -> Items.COAL, 2, 5, 3)
            )),
            new Tier("Deep Wilds", 2, List.of(
                    reward(() -> ModItems.SILK.get(), 2, 4, 6),
                    reward(() -> ModItems.SULFUR.get(), 2, 4, 5),
                    reward(() -> ModItems.FOSSIL_FRAGMENT.get(), 1, 2, 4),
                    reward(() -> ModItems.NESTING_TREAT.get(), 1, 1, 2),
                    reward(() -> ModItems.PTERANODON_WING_FRAGMENT.get(), 1, 1, 2),
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
                    reward(() -> ModItems.RAW_ANCIENT_SPELL_INGOT.get(), 1, 1, 3),
                    reward(() -> ModItems.COMPRESSED_CORE.get(), 1, 1, 1),
                    reward(() -> ModItems.CORE.get(), 2, 4, 7),
                    reward(() -> ModItems.PTERANODON_WING_FRAGMENT.get(), 1, 2, 3),
                    reward(() -> ModItems.TYRANNOSAURUS_TOOTH.get(), 1, 2, 3),
                    reward(() -> ModItems.SILK.get(), 3, 6, 3),
                    reward(() -> ModItems.FOSSIL_FRAGMENT.get(), 1, 3, 2),
                    reward(() -> ModItems.NESTING_TREAT.get(), 1, 2, 2)
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
        int rolls = definition.rolls();
        float bonusChance = Math.min(0.55F,
                Math.max(0, gatheringStars) * 0.08F + Math.max(0.0F, tableRewardMultiplier - 1.0F));
        if (random.nextFloat() < bonusChance) rolls++;

        Map<Item, ItemStack> combined = new LinkedHashMap<>();
        for (int roll = 0; roll < rolls; roll++) {
            Reward reward = choose(definition.rewards(), random);
            Item item = reward.item().get();
            int count = reward.minimum() + random.nextInt(reward.maximum() - reward.minimum() + 1);
            combined.compute(item, (ignored, existing) -> {
                if (existing == null) return new ItemStack(item, count);
                existing.grow(count);
                return existing;
            });
        }
        return new ArrayList<>(combined.values());
    }

    public static String shortPoolDescription(int tier) {
        return switch (Math.max(0, Math.min(4, tier))) {
            case 0 -> "Berries, feathers, seeds, fossil fragments";
            case 1 -> "Sulfur, feathers, fossils, hardwood, coal";
            case 2 -> "Silk, sulfur, nesting treats, wing fragments, cores, rare metal";
            case 3 -> "Teeth, wings, nesting treats, cores, ancient metal";
            default -> "Ancient ore, cores, rare compressed cores, trophies, nesting treats";
        };
    }

    private static Reward choose(List<Reward> rewards, RandomSource random) {
        int totalWeight = rewards.stream().mapToInt(Reward::weight).sum();
        int value = random.nextInt(totalWeight);
        for (Reward reward : rewards) {
            value -= reward.weight();
            if (value < 0) return reward;
        }
        return rewards.getLast();
    }

    private static Reward reward(Supplier<Item> item, int minimum, int maximum, int weight) {
        return new Reward(item, minimum, maximum, weight);
    }

    public record Tier(String name, int rolls, List<Reward> rewards) {
    }

    public record Reward(Supplier<Item> item, int minimum, int maximum, int weight) {
    }
}
