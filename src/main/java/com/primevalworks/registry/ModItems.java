package com.primevalworks.registry;

import com.primevalworks.PrimevalWorks;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.primevalworks.world.item.DinosaurEggItem;
import com.primevalworks.world.item.DinosaurEggBlockItem;

import java.util.List;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PrimevalWorks.MOD_ID);

    public static final DeferredItem<Item> FOSSIL_FRAGMENT = ITEMS.registerSimpleItem("fossil_fragment");
    public static final DeferredItem<SpawnEggItem> TYRANNOSAURUS_SPAWN_EGG = ITEMS.registerItem(
            "tyrannosaurus_spawn_egg",
            properties -> new DinosaurEggItem(properties.spawnEgg(ModEntities.TYRANNOSAURUS.get()))
    );
    public static final DeferredItem<SpawnEggItem> TRICERATOPS_SPAWN_EGG = ITEMS.registerItem(
            "triceratops_spawn_egg",
            properties -> new DinosaurEggItem(properties.spawnEgg(ModEntities.TRICERATOPS.get()))
    );
    public static final DeferredItem<SpawnEggItem> VELOCIRAPTOR_SPAWN_EGG = ITEMS.registerItem(
            "velociraptor_spawn_egg",
            properties -> new DinosaurEggItem(properties.spawnEgg(ModEntities.VELOCIRAPTOR.get()))
    );
    public static final DeferredItem<SpawnEggItem> STEGOSAURUS_SPAWN_EGG = ITEMS.registerItem(
            "stegosaurus_spawn_egg",
            properties -> new DinosaurEggItem(properties.spawnEgg(ModEntities.STEGOSAURUS.get()))
    );
    public static final DeferredItem<SpawnEggItem> PARASAUROLOPHUS_SPAWN_EGG = ITEMS.registerItem(
            "parasaurolophus_spawn_egg",
            properties -> new DinosaurEggItem(properties.spawnEgg(ModEntities.PARASAUROLOPHUS.get()))
    );
    public static final DeferredItem<SpawnEggItem> PTERANODON_SPAWN_EGG = ITEMS.registerItem(
            "pteranodon_spawn_egg",
            properties -> new DinosaurEggItem(properties.spawnEgg(ModEntities.PTERANODON.get()))
    );
    public static final DeferredItem<SpawnEggItem> FIELD_DODO_SPAWN_EGG = ITEMS.registerItem(
            "field_dodo_spawn_egg",
            properties -> new DinosaurEggItem(properties.spawnEgg(ModEntities.FIELD_DODO.get()))
    );
    public static final DeferredItem<SpawnEggItem> SPINOSAURUS_SPAWN_EGG = ITEMS.registerItem(
            "spinosaurus_spawn_egg",
            properties -> new DinosaurEggItem(properties.spawnEgg(ModEntities.SPINOSAURUS.get()))
    );
    public static final List<DeferredItem<SpawnEggItem>> DINOSAUR_DEBUG_SPAWN_EGGS = List.of(
            TYRANNOSAURUS_SPAWN_EGG, TRICERATOPS_SPAWN_EGG,
            VELOCIRAPTOR_SPAWN_EGG, STEGOSAURUS_SPAWN_EGG,
            PARASAUROLOPHUS_SPAWN_EGG, PTERANODON_SPAWN_EGG,
            FIELD_DODO_SPAWN_EGG, SPINOSAURUS_SPAWN_EGG
    );
    public static final List<DeferredItem<SpawnEggItem>> PLAYABLE_DINOSAUR_SPAWN_EGGS = List.of(
            TYRANNOSAURUS_SPAWN_EGG, TRICERATOPS_SPAWN_EGG, VELOCIRAPTOR_SPAWN_EGG,
            STEGOSAURUS_SPAWN_EGG, PARASAUROLOPHUS_SPAWN_EGG, PTERANODON_SPAWN_EGG,
            FIELD_DODO_SPAWN_EGG, SPINOSAURUS_SPAWN_EGG
    );
    public static final DeferredItem<BlockItem> SMALL_DINOSAUR_EGG = ITEMS.registerItem(
            "small_dinosaur_egg", properties -> new DinosaurEggBlockItem(ModBlocks.SMALL_DINOSAUR_EGG.get(), properties));
    public static final DeferredItem<BlockItem> BIG_DINOSAUR_EGG = ITEMS.registerItem(
            "big_dinosaur_egg", properties -> new DinosaurEggBlockItem(ModBlocks.BIG_DINOSAUR_EGG.get(), properties));
    public static final DeferredItem<BlockItem> LARGE_DINOSAUR_EGG = ITEMS.registerItem(
            "large_dinosaur_egg", properties -> new DinosaurEggBlockItem(ModBlocks.LARGE_DINOSAUR_EGG.get(), properties));
    public static final DeferredItem<BlockItem> COMMAND_TABLE =
            ITEMS.registerSimpleBlockItem("command_table", ModBlocks.COMMAND_TABLE);
    public static final DeferredItem<BlockItem> FOOD_BOX =
            ITEMS.registerSimpleBlockItem("food_box", ModBlocks.FOOD_BOX);
    public static final DeferredItem<BlockItem> WIND_TURBINE =
            ITEMS.registerSimpleBlockItem("wind_turbine", ModBlocks.WIND_TURBINE);
    public static final DeferredItem<BlockItem> WATER_TURBINE =
            ITEMS.registerSimpleBlockItem("water_turbine", ModBlocks.WATER_TURBINE);
    public static final DeferredItem<BlockItem> REINFORCED_PISTON =
            ITEMS.registerSimpleBlockItem("reinforced_piston", ModBlocks.REINFORCED_PISTON);
    public static final DeferredItem<BlockItem> STICKY_REINFORCED_PISTON =
            ITEMS.registerSimpleBlockItem("sticky_reinforced_piston", ModBlocks.STICKY_REINFORCED_PISTON);
    public static final DeferredItem<BlockItem> LASER_OBSERVER =
            ITEMS.registerSimpleBlockItem("laser_observer", ModBlocks.LASER_OBSERVER);
    public static final DeferredItem<BlockItem> ANCIENT_BARREL =
            ITEMS.registerSimpleBlockItem("ancient_barrel", ModBlocks.ANCIENT_BARREL);
    public static final DeferredItem<BlockItem> DART_TURRET =
            ITEMS.registerSimpleBlockItem("dart_turret", ModBlocks.DART_TURRET);
    public static final DeferredItem<BlockItem> PROCESSOR =
            ITEMS.registerSimpleBlockItem("processor", ModBlocks.PROCESSOR);
    public static final DeferredItem<BlockItem> ANCIENT_FURNACE =
            ITEMS.registerSimpleBlockItem("ancient_furnace", ModBlocks.ANCIENT_FURNACE);
    public static final DeferredItem<BlockItem> ANCIENT_SPELL_STONE =
            ITEMS.registerSimpleBlockItem("ancient_spell_stone", ModBlocks.ANCIENT_SPELL_STONE);
    public static final DeferredItem<BlockItem> MAGIC_TURRET =
            ITEMS.registerSimpleBlockItem("magic_turret", ModBlocks.MAGIC_TURRET);
    public static final DeferredItem<BlockItem> SPINOSAURUS_HEAD =
            ITEMS.registerSimpleBlockItem("spinosaurus_head", ModBlocks.SPINOSAURUS_HEAD);
    public static final DeferredItem<BlockItem> PREMIUM_EGG_INCUBATOR =
            ITEMS.registerSimpleBlockItem("premium_egg_incubator", ModBlocks.PREMIUM_EGG_INCUBATOR);
    public static final DeferredItem<Item> PTERANODON_SADDLE = ITEMS.registerItem(
            "pteranodon_saddle", properties -> new Item(properties.stacksTo(1).fireResistant())
    );
    public static final DeferredItem<Item> SPINOSAURUS_SADDLE = ITEMS.registerItem(
            "spinosaurus_saddle", properties -> new Item(properties.stacksTo(1).fireResistant())
    );
    public static final DeferredItem<Item> PRIMORDIAL_SWORD = ITEMS.registerItem(
            "primordial_sword",
            properties -> new Item(ToolMaterial.NETHERITE.applySwordProperties(properties, 6.0F, -2.2F))
    );
    public static final DeferredItem<Item> RAW_ANCIENT_SPELL_INGOT = ITEMS.registerSimpleItem("raw_ancient_spell_ingot");
    public static final DeferredItem<Item> ANCIENT_SPELL_INGOT = ITEMS.registerSimpleItem("ancient_spell_ingot");
    public static final DeferredItem<Item> HARDWOOD = ITEMS.registerSimpleItem("hardwood");
    public static final DeferredItem<Item> SILK = ITEMS.registerSimpleItem("silk");
    public static final DeferredItem<Item> RAW_ANCIENT_METAL_INGOT = ITEMS.registerSimpleItem("raw_ancient_metal_ingot");
    public static final DeferredItem<Item> ANCIENT_METAL_INGOT = ITEMS.registerSimpleItem("ancient_metal_ingot");
    public static final DeferredItem<Item> COMPRESSED_ANCIENT_METAL_INGOT = ITEMS.registerSimpleItem("compressed_ancient_metal_ingot");
    public static final DeferredItem<Item> SULFUR = ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<Item> BIG_DINO_BONE = ITEMS.registerSimpleItem("big_dino_bone");
    public static final DeferredItem<Item> SMALL_DINO_BONE = ITEMS.registerSimpleItem("small_dino_bone");
    public static final DeferredItem<Item> PTERANODON_WING_FRAGMENT = ITEMS.registerSimpleItem("pteranodon_wing_fragment");
    public static final DeferredItem<Item> TYRANNOSAURUS_TOOTH = ITEMS.registerSimpleItem("tyrannosaurus_tooth");
    public static final DeferredItem<Item> DODO_FEATHER = ITEMS.registerSimpleItem("dodo_feather");
    public static final DeferredItem<Item> CORE = ITEMS.registerSimpleItem("core");
    public static final DeferredItem<Item> COMPRESSED_CORE = ITEMS.registerSimpleItem("compressed_core");
    public static final DeferredItem<Item> NESTING_TREAT = ITEMS.registerSimpleItem("nesting_treat");
    public static final DeferredItem<Item> DART = ITEMS.registerSimpleItem("dart");
    public static final DeferredItem<BlockItem> BERRIES = ITEMS.registerItem(
            "berries",
            properties -> new BlockItem(ModBlocks.BERRY_BUSH.get(),
                    properties.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()))
    );
    public static final DeferredItem<Item> BIG_DINO_MEAT = ITEMS.registerItem(
            "big_dino_meat",
            properties -> new Item(properties.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.4F).build()))
    );
    public static final DeferredItem<Item> SMALL_DINO_MEAT = ITEMS.registerItem(
            "small_dino_meat",
            properties -> new Item(properties.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()))
    );
    public static final DeferredItem<Item> COOKED_DINO_MEAT = ITEMS.registerItem(
            "cooked_dino_meat",
            properties -> new Item(properties.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()))
    );
    public static final DeferredItem<Item> COOKED_LARGE_DINO_MEAT = ITEMS.registerItem(
            "cooked_large_dino_meat",
            properties -> new Item(properties.food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.9F).build()))
    );
    public static final DeferredItem<Item> ROASTED_BEET = ITEMS.registerItem(
            "roasted_beet",
            properties -> new Item(properties.food(new FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.8F)
                    .build()))
    );
    public static final DeferredItem<Item> FIRE_ROASTED_MELON = ITEMS.registerItem(
            "fire_roasted_melon",
            properties -> new Item(properties.food(new FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.6F)
                    .build()))
    );

    private ModItems() {
    }

    public static boolean isDinosaurEgg(net.minecraft.world.item.ItemStack stack) {
        return stack.is(SMALL_DINOSAUR_EGG.get())
                || stack.is(BIG_DINOSAUR_EGG.get())
                || stack.is(LARGE_DINOSAUR_EGG.get());
    }

    public static boolean isDebugSpawnEgg(Item item) {
        return DINOSAUR_DEBUG_SPAWN_EGGS.stream().anyMatch(egg -> egg.get() == item);
    }

    public static boolean isPlayableSpawnEgg(Item item) {
        return PLAYABLE_DINOSAUR_SPAWN_EGGS.stream().anyMatch(egg -> egg.get() == item);
    }

    public static void register(IEventBus modBus) {
        ITEMS.addAlias(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "laser_turret"),
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "magic_turret")
        );
        ITEMS.addAlias(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "enhanced_rail"),
                Identifier.fromNamespaceAndPath("minecraft", "powered_rail")
        );
        ITEMS.addAlias(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "ancient_reforged_bayonet"),
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "primordial_sword")
        );
        ITEMS.register(modBus);
    }
}
