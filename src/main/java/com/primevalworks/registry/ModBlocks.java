package com.primevalworks.registry;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.block.AncientBarrelBlock;
import com.primevalworks.world.block.CommandTableExtensionBlock;
import com.primevalworks.world.block.AncientFurnaceBlock;
import com.primevalworks.world.block.FoodBoxBlock;
import com.primevalworks.world.block.PremiumEggIncubatorBlock;
import com.primevalworks.world.block.PoweredMachineBlock;
import com.primevalworks.world.block.ProcessorBlock;
import com.primevalworks.world.block.PoweredObserverBlock;
import com.primevalworks.world.block.PoweredPistonBlock;
import com.primevalworks.world.block.PrimevalBerryBushBlock;
import com.primevalworks.world.block.TurbineBlock;
import com.primevalworks.world.block.TurbinePartBlock;
import com.primevalworks.world.block.DinosaurEggBlock;
import com.primevalworks.world.block.DartTurretBlock;
import com.primevalworks.world.block.LaserTurretBlock;
import com.primevalworks.world.block.SpinosaurusHeadBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PrimevalWorks.MOD_ID);

    public static final DeferredBlock<Block> COMMAND_TABLE = BLOCKS.registerBlock(
            "command_table",
             properties -> new CommandTableBlock(properties
                     .mapColor(MapColor.COLOR_BROWN)
                     .strength(50.0F, 1200.0F)
                     .noOcclusion()
                     .sound(SoundType.STONE))
    );
    public static final DeferredBlock<Block> COMMAND_TABLE_EXTENSION = BLOCKS.registerBlock(
            "command_table_extension",
            properties -> new CommandTableExtensionBlock(properties
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(3.5F)
                    .noCollision()
                    .noOcclusion()
                    .sound(SoundType.WOOD))
    );
    public static final DeferredBlock<Block> FOOD_BOX = BLOCKS.registerBlock(
            "food_box",
            properties -> new FoodBoxBlock(properties
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.5F)
                    .sound(SoundType.WOOD))
    );
    public static final DeferredBlock<Block> WIND_TURBINE = BLOCKS.registerBlock(
            "wind_turbine",
            properties -> new TurbineBlock(properties.mapColor(MapColor.METAL).strength(4.0F)
                    .noOcclusion().sound(SoundType.METAL))
    );
    public static final DeferredBlock<Block> UPGRADED_WIND_TURBINE = BLOCKS.registerBlock(
            "upgraded_wind_turbine",
            properties -> new TurbineBlock(properties.mapColor(MapColor.METAL).strength(5.0F)
                    .noOcclusion().sound(SoundType.METAL))
    );
    public static final DeferredBlock<Block> WATER_TURBINE = BLOCKS.registerBlock(
            "water_turbine",
            properties -> new TurbineBlock(properties.mapColor(MapColor.COLOR_BROWN).strength(4.0F)
                    .noOcclusion().sound(SoundType.WOOD))
    );
    public static final DeferredBlock<Block> TURBINE_PART = BLOCKS.registerBlock(
            "turbine_part",
            properties -> new TurbinePartBlock(properties.mapColor(MapColor.NONE).strength(4.0F)
                    .noOcclusion().noLootTable().sound(SoundType.METAL))
    );
    public static final DeferredBlock<Block> REINFORCED_PISTON = BLOCKS.registerBlock(
            "reinforced_piston",
            properties -> new PoweredPistonBlock(false, properties.mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE))
    );
    public static final DeferredBlock<Block> STICKY_REINFORCED_PISTON = BLOCKS.registerBlock(
            "sticky_reinforced_piston",
            properties -> new PoweredPistonBlock(true, properties.mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE))
    );
    public static final DeferredBlock<Block> LASER_OBSERVER = BLOCKS.registerBlock(
            "laser_observer",
            properties -> new PoweredObserverBlock(properties.mapColor(MapColor.METAL).strength(3.5F)
                    .noOcclusion().sound(SoundType.METAL))
    );
    public static final DeferredBlock<Block> ANCIENT_BARREL = BLOCKS.registerBlock(
            "ancient_barrel",
            properties -> new AncientBarrelBlock(properties.mapColor(MapColor.COLOR_BROWN)
                    .strength(4.0F).sound(SoundType.WOOD))
    );
    public static final DeferredBlock<Block> DART_TURRET = BLOCKS.registerBlock(
            "dart_turret",
            properties -> new DartTurretBlock(properties.mapColor(MapColor.METAL).strength(4.5F)
                    .noOcclusion().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> PROCESSOR = BLOCKS.registerBlock(
            "processor",
            properties -> new ProcessorBlock(properties.mapColor(MapColor.METAL).strength(5.0F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(ProcessorBlock.PROCESSING) ? 7 : 0)
                    .sound(SoundType.METAL))
    );
    public static final DeferredBlock<Block> ANCIENT_FURNACE = BLOCKS.registerBlock(
            "ancient_furnace",
            properties -> new AncientFurnaceBlock(properties
                    .mapColor(MapColor.METAL)
                    .strength(5.0F)
                    .lightLevel(state -> state.getValue(AncientFurnaceBlock.LIT) ? 13 : 0)
                    .sound(SoundType.METAL))
    );
    public static final DeferredBlock<Block> ANCIENT_SPELL_STONE = BLOCKS.registerBlock(
            "ancient_spell_stone",
            properties -> new PoweredMachineBlock(properties.mapColor(MapColor.COLOR_PURPLE).strength(7.0F)
                    .noOcclusion().lightLevel(state -> 8).sound(SoundType.AMETHYST))
    );
    public static final DeferredBlock<Block> LASER_TURRET = BLOCKS.registerBlock(
            "laser_turret",
            properties -> new LaserTurretBlock(properties.mapColor(MapColor.METAL).strength(7.0F)
                    .noOcclusion().lightLevel(state -> 7).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> SPINOSAURUS_HEAD = BLOCKS.registerBlock(
            "spinosaurus_head",
            properties -> new SpinosaurusHeadBlock(properties.mapColor(MapColor.COLOR_BROWN).strength(5.0F)
                    .noOcclusion().sound(SoundType.BONE_BLOCK)));
    public static final DeferredBlock<Block> PREMIUM_EGG_INCUBATOR = BLOCKS.registerBlock(
            "premium_egg_incubator",
            properties -> new PremiumEggIncubatorBlock(properties.mapColor(MapColor.QUARTZ).strength(5.0F)
                    .noOcclusion().lightLevel(state -> 6).sound(SoundType.GLASS))
    );
    public static final DeferredBlock<PrimevalBerryBushBlock> BERRY_BUSH = BLOCKS.registerBlock(
            "berry_bush",
            properties -> new PrimevalBerryBushBlock(properties.mapColor(MapColor.PLANT)
                    .randomTicks().noCollision().noOcclusion().instabreak().sound(SoundType.SWEET_BERRY_BUSH))
    );
    public static final DeferredBlock<DinosaurEggBlock.Small> SMALL_DINOSAUR_EGG = BLOCKS.registerBlock(
            "small_dinosaur_egg",
            properties -> new DinosaurEggBlock.Small(eggProperties(properties))
    );
    public static final DeferredBlock<DinosaurEggBlock.Big> BIG_DINOSAUR_EGG = BLOCKS.registerBlock(
            "big_dinosaur_egg",
            properties -> new DinosaurEggBlock.Big(eggProperties(properties))
    );
    public static final DeferredBlock<DinosaurEggBlock.Large> LARGE_DINOSAUR_EGG = BLOCKS.registerBlock(
            "large_dinosaur_egg",
            properties -> new DinosaurEggBlock.Large(eggProperties(properties))
    );

    private static DeferredBlock<Block> simpleMachine(String name, MapColor color, SoundType sound, float strength) {
        return BLOCKS.registerBlock(name, properties -> new PoweredMachineBlock(properties.mapColor(color).strength(strength)
                .noOcclusion().sound(sound)));
    }

    private static Block.Properties eggProperties(Block.Properties properties) {
        return properties
                .mapColor(MapColor.QUARTZ)
                .strength(0.35F)
                .noOcclusion()
                .sound(SoundType.BONE_BLOCK);
    }

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.addAlias(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "magic_turret"),
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "laser_turret")
        );
        BLOCKS.addAlias(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "enhanced_rail"),
                Identifier.fromNamespaceAndPath("minecraft", "powered_rail")
        );
        BLOCKS.register(modBus);
    }
}
