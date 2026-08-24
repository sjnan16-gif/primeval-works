package com.primevalworks.registry;

import com.primevalworks.PrimevalWorks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> FIELD_DODO_FOOD = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "field_dodo_food")
    );
    public static final TagKey<Item> HERBIVORE_FOOD = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "herbivore_food")
    );
    public static final TagKey<Item> CARNIVORE_FOOD = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "carnivore_food")
    );

    private ModItemTags() {
    }
}
