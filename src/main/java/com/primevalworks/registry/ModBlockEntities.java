package com.primevalworks.registry;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.block.entity.AncientFurnaceBlockEntity;
import com.primevalworks.world.block.entity.AncientBarrelBlockEntity;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.block.entity.FoodBoxBlockEntity;
import com.primevalworks.world.block.entity.TurbineBlockEntity;
import com.primevalworks.world.block.entity.PremiumEggIncubatorBlockEntity;
import com.primevalworks.world.block.entity.ProcessorBlockEntity;
import com.primevalworks.world.block.entity.LaserTurretBlockEntity;
import com.primevalworks.world.block.entity.LaserObserverBlockEntity;
import com.primevalworks.world.block.entity.SpinosaurusHeadBlockEntity;
import com.primevalworks.world.block.entity.DinosaurEggBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PrimevalWorks.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CommandTableBlockEntity>> COMMAND_TABLE =
            BLOCK_ENTITIES.register("command_table", () -> new BlockEntityType<>(
                    CommandTableBlockEntity::new,
                    ModBlocks.COMMAND_TABLE.get()
            ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AncientFurnaceBlockEntity>> ANCIENT_FURNACE =
            BLOCK_ENTITIES.register("ancient_furnace", () -> new BlockEntityType<>(
                    AncientFurnaceBlockEntity::new,
                    ModBlocks.ANCIENT_FURNACE.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoodBoxBlockEntity>> FOOD_BOX =
            BLOCK_ENTITIES.register("food_box", () -> new BlockEntityType<>(
                    FoodBoxBlockEntity::new,
                    ModBlocks.FOOD_BOX.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AncientBarrelBlockEntity>> ANCIENT_BARREL =
            BLOCK_ENTITIES.register("ancient_barrel", () -> new BlockEntityType<>(
                    AncientBarrelBlockEntity::new,
                    ModBlocks.ANCIENT_BARREL.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineBlockEntity>> TURBINE =
            BLOCK_ENTITIES.register("turbine", () -> new BlockEntityType<>(
                    TurbineBlockEntity::new,
                    ModBlocks.WIND_TURBINE.get(),
                    ModBlocks.UPGRADED_WIND_TURBINE.get(),
                    ModBlocks.WATER_TURBINE.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PremiumEggIncubatorBlockEntity>> PREMIUM_EGG_INCUBATOR =
            BLOCK_ENTITIES.register("premium_egg_incubator", () -> new BlockEntityType<>(
                    PremiumEggIncubatorBlockEntity::new,
                    ModBlocks.PREMIUM_EGG_INCUBATOR.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DinosaurEggBlockEntity>> DINOSAUR_EGG =
            BLOCK_ENTITIES.register("dinosaur_egg", () -> new BlockEntityType<>(
                    DinosaurEggBlockEntity::new,
                    ModBlocks.SMALL_DINOSAUR_EGG.get(),
                    ModBlocks.BIG_DINOSAUR_EGG.get(),
                    ModBlocks.LARGE_DINOSAUR_EGG.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessorBlockEntity>> PROCESSOR =
            BLOCK_ENTITIES.register("processor", () -> new BlockEntityType<>(
                    ProcessorBlockEntity::new,
                    ModBlocks.PROCESSOR.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaserTurretBlockEntity>> LASER_TURRET =
            BLOCK_ENTITIES.register("laser_turret", () -> new BlockEntityType<>(
                    LaserTurretBlockEntity::new,
                    ModBlocks.LASER_TURRET.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpinosaurusHeadBlockEntity>> SPINOSAURUS_HEAD =
            BLOCK_ENTITIES.register("spinosaurus_head", () -> new BlockEntityType<>(
                    SpinosaurusHeadBlockEntity::new,
                    ModBlocks.SPINOSAURUS_HEAD.get()
            ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaserObserverBlockEntity>> LASER_OBSERVER =
            BLOCK_ENTITIES.register("laser_observer", () -> new BlockEntityType<>(
                    LaserObserverBlockEntity::new,
                    ModBlocks.LASER_OBSERVER.get()
            ));
    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.addAlias(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "magic_turret"),
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "laser_turret")
        );
        BLOCK_ENTITIES.register(modBus);
    }
}
