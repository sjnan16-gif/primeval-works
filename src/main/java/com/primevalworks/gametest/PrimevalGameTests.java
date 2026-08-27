package com.primevalworks.gametest;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.registry.ModBlocks;
import com.primevalworks.registry.ModBlockEntities;
import com.primevalworks.registry.ModEntities;
import com.primevalworks.registry.ModItems;
import com.primevalworks.world.block.entity.TurbineBlockEntity;
import com.primevalworks.world.block.entity.PremiumEggIncubatorBlockEntity;
import com.primevalworks.world.block.entity.CommandTableBlockEntity;
import com.primevalworks.world.block.entity.FoodBoxBlockEntity;
import com.primevalworks.world.block.entity.ProcessorBlockEntity;
import com.primevalworks.world.block.entity.AncientFurnaceBlockEntity;
import com.primevalworks.world.block.entity.DartTurretBlockEntity;
import com.primevalworks.world.block.entity.LaserTurretBlockEntity;
import com.primevalworks.world.block.CommandTableBlock;
import com.primevalworks.world.block.BeamLineOfSight;
import com.primevalworks.world.block.PoweredObserverBlock;
import com.primevalworks.world.block.TurbineBlock;
import com.primevalworks.world.block.TurbinePartBlock;
import com.primevalworks.world.base.BaseUpgrade;
import com.primevalworks.world.base.BaseEnergyRules;
import com.primevalworks.world.entity.DinosaurMutationRules;
import com.primevalworks.world.entity.DinosaurGeneticPerformanceRules;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.egg.DinosaurEggGenome;
import com.primevalworks.world.egg.DinosaurEggSize;
import com.primevalworks.world.egg.DinosaurHatching;
import com.primevalworks.world.ownership.DinosaurOwnership;
import com.primevalworks.world.inventory.FoodBoxMenu;
import com.primevalworks.world.work.BaseInventoryIndex;
import com.primevalworks.world.work.ExpeditionRewards;
import com.primevalworks.world.work.WorkSpecialtyRules;
import com.primevalworks.world.work.DinosaurCommandMode;
import com.primevalworks.world.work.DinoWhistleSettings;
import com.primevalworks.world.processor.ProcessorRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class PrimevalGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, PrimevalWorks.MOD_ID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENERGY_WORK_CYCLE =
            TEST_FUNCTIONS.register("energy_work_cycle", () -> PrimevalGameTests::energyWorkCycle);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENERGY_WORKER_STAYS_ON_DUTY =
            TEST_FUNCTIONS.register("energy_worker_stays_on_duty", () -> PrimevalGameTests::energyWorkerStaysOnDuty);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORK_ORDER_SURVIVES_ROSTER_ROUND_TRIP =
            TEST_FUNCTIONS.register("work_order_survives_roster_round_trip", () -> PrimevalGameTests::workOrderSurvivesRosterRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ACTIVE_WORK_RESTORES_AFTER_LOGIN =
            TEST_FUNCTIONS.register("active_work_restores_after_login", () -> PrimevalGameTests::activeWorkRestoresAfterLogin);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONTENT_REGISTRATION =
            TEST_FUNCTIONS.register("content_registration", () -> PrimevalGameTests::contentRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HOSTILE_TARGETS_BASE_DINOSAUR =
            TEST_FUNCTIONS.register("hostile_targets_base_dinosaur", () -> PrimevalGameTests::hostileTargetsBaseDinosaur);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TYRANNOSAURUS_HUNTS_FROM_MOUTH_RANGE =
            TEST_FUNCTIONS.register("tyrannosaurus_hunts_from_mouth_range", () -> PrimevalGameTests::tyrannosaurusHuntsFromMouthRange);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VELOCIRAPTOR_ATTACK_LANDS_ON_CONTACT =
            TEST_FUNCTIONS.register("velociraptor_attack_lands_on_contact",
                    () -> PrimevalGameTests::velociraptorAttackLandsOnContact);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VELOCIRAPTOR_WALKS_WITHOUT_PURSUIT =
            TEST_FUNCTIONS.register("velociraptor_walks_without_pursuit",
                    () -> PrimevalGameTests::velociraptorWalksWithoutPursuit);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SPINOSAURUS_CLEARS_CLOSE_TARGET =
            TEST_FUNCTIONS.register("spinosaurus_clears_close_target", () -> PrimevalGameTests::spinosaurusClearsCloseTarget);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NIGHT_SHIFT_DRAINS_MOOD =
            TEST_FUNCTIONS.register("night_shift_drains_mood", () -> PrimevalGameTests::nightShiftDrainsMood);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DINOSAUR_SLEEPS_AT_NIGHT =
            TEST_FUNCTIONS.register("dinosaur_sleeps_at_night", () -> PrimevalGameTests::dinosaurSleepsAtNight);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INCUBATOR_IMPROVES_HATCHLING =
            TEST_FUNCTIONS.register("incubator_improves_hatchling", () -> PrimevalGameTests::incubatorImprovesHatchling);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WILD_EGG_HATCHES =
            TEST_FUNCTIONS.register("wild_egg_hatches", () -> PrimevalGameTests::wildEggHatches);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PRE_TABLE_HATCH_IS_OWNED =
            TEST_FUNCTIONS.register("pre_table_hatch_is_owned", () -> PrimevalGameTests::preTableHatchIsOwned);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DODO_TRANSPORTS_BETWEEN_CHESTS =
            TEST_FUNCTIONS.register("dodo_transports_between_chests", () -> PrimevalGameTests::dodoTransportsBetweenChests);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BASE_UPGRADES_APPLY =
            TEST_FUNCTIONS.register("base_upgrades_apply", () -> PrimevalGameTests::baseUpgradesApply);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COMMAND_TABLE_USES_ONE_BLOCK =
            TEST_FUNCTIONS.register("command_table_uses_one_block", () -> PrimevalGameTests::commandTableUsesOneBlock);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STORE_ALL_RETURNS_ACTIVE_DINOSAURS =
            TEST_FUNCTIONS.register("store_all_returns_active_dinosaurs", () -> PrimevalGameTests::storeAllReturnsActiveDinosaurs);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INDIVIDUAL_DINOSAUR_RECALL =
            TEST_FUNCTIONS.register("individual_dinosaur_recall", () -> PrimevalGameTests::individualDinosaurRecall);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PTERANODON_SADDLE_MOUNTS =
            TEST_FUNCTIONS.register("pteranodon_saddle_mounts", () -> PrimevalGameTests::pteranodonSaddleMounts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEFEATED_DINOSAUR_RETURNS_TO_DEPOT =
            TEST_FUNCTIONS.register("defeated_dinosaur_returns_to_depot", () -> PrimevalGameTests::defeatedDinosaurReturnsToDepot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COMMAND_KILLED_DINOSAUR_RECOVERS =
            TEST_FUNCTIONS.register("command_killed_dinosaur_recovers", () -> PrimevalGameTests::commandKilledDinosaurRecovers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BASE_INVENTORY_INDEXES_CHEST_CONTENTS =
            TEST_FUNCTIONS.register("base_inventory_indexes_chest_contents", () -> PrimevalGameTests::baseInventoryIndexesChestContents);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STEGOSAURUS_TENDS_FURNACE =
            TEST_FUNCTIONS.register("stegosaurus_tends_furnace", () -> PrimevalGameTests::stegosaurusTendsFurnace);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DINOSAUR_CRAFTS_FROM_BASE_STORAGE =
            TEST_FUNCTIONS.register("dinosaur_crafts_from_base_storage", () -> PrimevalGameTests::dinosaurCraftsFromBaseStorage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRAFTING_CANCELS_IF_INGREDIENTS_CHANGE =
            TEST_FUNCTIONS.register("crafting_cancels_if_ingredients_change", () -> PrimevalGameTests::craftingCancelsIfIngredientsChange);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXPEDITION_POOLS_GATE_ANCIENT_METAL =
            TEST_FUNCTIONS.register("expedition_pools_gate_ancient_metal", () -> PrimevalGameTests::expeditionPoolsGateAncientMetal);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FOOD_BOX_ACCEPTS_ALL_DINOSAUR_FOOD =
            TEST_FUNCTIONS.register("food_box_accepts_all_dinosaur_food", () -> PrimevalGameTests::foodBoxAcceptsAllDinosaurFood);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HUNGRY_DINOSAUR_EATS_FROM_FOOD_BOX =
            TEST_FUNCTIONS.register("hungry_dinosaur_eats_from_food_box", () -> PrimevalGameTests::hungryDinosaurEatsFromFoodBox);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TURBINES_USE_FULL_STRUCTURE =
            TEST_FUNCTIONS.register("turbines_use_full_structure", () -> PrimevalGameTests::turbinesUseFullStructure);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BASE_ENERGY_STORES_AND_DRAINS =
            TEST_FUNCTIONS.register("base_energy_stores_and_drains", () -> PrimevalGameTests::baseEnergyStoresAndDrains);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENERGY_STATION_REJECTS_SECOND_WORKER =
            TEST_FUNCTIONS.register("energy_station_rejects_second_worker", () -> PrimevalGameTests::energyStationRejectsSecondWorker);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> POWERED_PISTON_REQUIRES_BASE_ENERGY =
            TEST_FUNCTIONS.register("powered_piston_requires_base_energy", () -> PrimevalGameTests::poweredPistonRequiresBaseEnergy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> POWERED_INTERACTION_REQUIRES_ENERGY =
            TEST_FUNCTIONS.register("powered_interaction_requires_energy", () -> PrimevalGameTests::poweredInteractionRequiresEnergy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HUGE_AND_ALBINO_MODIFIERS =
            TEST_FUNCTIONS.register("huge_and_albino_modifiers", () -> PrimevalGameTests::hugeAndAlbinoModifiers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FOSSIL_RESTORES_ALBINO_APPEARANCE =
            TEST_FUNCTIONS.register("fossil_restores_albino_appearance", () -> PrimevalGameTests::fossilRestoresAlbinoAppearance);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PLAYER_KILL_PERMANENTLY_REMOVES_DINOSAUR =
            TEST_FUNCTIONS.register("player_kill_permanently_removes_dinosaur", () -> PrimevalGameTests::playerKillPermanentlyRemovesDinosaur);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROCESSOR_COMPRESSES_CORE =
            TEST_FUNCTIONS.register("processor_compresses_core", () -> PrimevalGameTests::processorCompressesCore);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WIND_TURBINE_UPGRADE_PATH =
            TEST_FUNCTIONS.register("wind_turbine_upgrade_path", () -> PrimevalGameTests::windTurbineUpgradePath);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROCESSOR_FINISHES_WITHOUT_WORKER =
            TEST_FUNCTIONS.register("processor_finishes_without_worker", () -> PrimevalGameTests::processorFinishesWithoutWorker);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROCESSOR_TRANSPORT_ROUND_TRIP =
            TEST_FUNCTIONS.register("processor_transport_round_trip", () -> PrimevalGameTests::processorTransportRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ANCIENT_FURNACE_USES_VARIABLE_ENERGY =
            TEST_FUNCTIONS.register("ancient_furnace_uses_variable_energy", () -> PrimevalGameTests::ancientFurnaceUsesVariableEnergy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROSTER_HEALTH_MIGRATES_ATTRIBUTES =
            TEST_FUNCTIONS.register("roster_health_migrates_attributes", () -> PrimevalGameTests::rosterHealthMigratesAttributes);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DINOSAURS_BREED_SAME_SPECIES =
            TEST_FUNCTIONS.register("dinosaurs_breed_same_species", () -> PrimevalGameTests::dinosaursBreedSameSpecies);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXPEDITION_RETURN_IS_ONE_SHOT =
            TEST_FUNCTIONS.register("expedition_return_is_one_shot", () -> PrimevalGameTests::expeditionReturnIsOneShot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> POWERED_TURRETS_DEFEND_BASE =
            TEST_FUNCTIONS.register("powered_turrets_defend_base", () -> PrimevalGameTests::poweredTurretsDefendBase);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXPEDITION_SLOT_SURVIVES_RELOAD =
            TEST_FUNCTIONS.register("expedition_slot_survives_reload", () -> PrimevalGameTests::expeditionSlotSurvivesReload);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LASER_OBSERVER_DETECTS_FIVE_BLOCKS =
            TEST_FUNCTIONS.register("laser_observer_detects_five_blocks", () -> PrimevalGameTests::laserObserverDetectsFiveBlocks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UNMOUNTED_SPINOSAURUS_FLOATS_AT_SURFACE =
            TEST_FUNCTIONS.register("unmounted_spinosaurus_floats_at_surface",
                    () -> PrimevalGameTests::unmountedSpinosaurusFloatsAtSurface);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HEAVY_DINOSAUR_REQUIRES_FLAT_SLEEP_AREA =
            TEST_FUNCTIONS.register("heavy_dinosaur_requires_flat_sleep_area",
                    () -> PrimevalGameTests::heavyDinosaurRequiresFlatSleepArea);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HIT_WORKER_RESUMES_ASSIGNMENT =
            TEST_FUNCTIONS.register("hit_worker_resumes_assignment",
                    () -> PrimevalGameTests::hitWorkerResumesAssignment);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FOLLOWER_ORDER_SURVIVES_RELOAD =
            TEST_FUNCTIONS.register("follower_order_survives_reload",
                    () -> PrimevalGameTests::followerOrderSurvivesReload);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FOLLOWER_QUARRIES_MARKED_BLOCK =
            TEST_FUNCTIONS.register("follower_quarries_marked_block",
                    () -> PrimevalGameTests::followerQuarriesMarkedBlock);

    private PrimevalGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(PrimevalGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "energy_work_cycle"),
                new FunctionGameTestInstance(ENERGY_WORK_CYCLE.getKey(), isolatedTestData(event, "energy"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "energy_worker_stays_on_duty"),
                new FunctionGameTestInstance(ENERGY_WORKER_STAYS_ON_DUTY.getKey(), isolatedTestData(event, "energy_continuous"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "work_order_survives_roster_round_trip"),
                new FunctionGameTestInstance(WORK_ORDER_SURVIVES_ROSTER_ROUND_TRIP.getKey(), isolatedTestData(event, "work_persistence"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "active_work_restores_after_login"),
                new FunctionGameTestInstance(ACTIVE_WORK_RESTORES_AFTER_LOGIN.getKey(), isolatedTestData(event, "login_work_restore"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "content_registration"),
                new FunctionGameTestInstance(CONTENT_REGISTRATION.getKey(), isolatedTestData(event, "content"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "hostile_targets_base_dinosaur"),
                new FunctionGameTestInstance(HOSTILE_TARGETS_BASE_DINOSAUR.getKey(), isolatedTestData(event, "combat"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "tyrannosaurus_hunts_from_mouth_range"),
                new FunctionGameTestInstance(TYRANNOSAURUS_HUNTS_FROM_MOUTH_RANGE.getKey(), isolatedTestData(event, "combat_spacing"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "velociraptor_attack_lands_on_contact"),
                new FunctionGameTestInstance(VELOCIRAPTOR_ATTACK_LANDS_ON_CONTACT.getKey(),
                        isolatedTestData(event, "raptor_combat"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "velociraptor_walks_without_pursuit"),
                new FunctionGameTestInstance(VELOCIRAPTOR_WALKS_WITHOUT_PURSUIT.getKey(),
                        isolatedTestData(event, "raptor_walk"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "spinosaurus_clears_close_target"),
                new FunctionGameTestInstance(SPINOSAURUS_CLEARS_CLOSE_TARGET.getKey(), isolatedTestData(event, "spino_close_combat"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "night_shift_drains_mood"),
                new FunctionGameTestInstance(NIGHT_SHIFT_DRAINS_MOOD.getKey(),
                        isolatedTestData(event, "night_shift", 2_400))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dinosaur_sleeps_at_night"),
                new FunctionGameTestInstance(DINOSAUR_SLEEPS_AT_NIGHT.getKey(), isolatedTestData(event, "sleep"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "incubator_improves_hatchling"),
                new FunctionGameTestInstance(INCUBATOR_IMPROVES_HATCHLING.getKey(), isolatedTestData(event, "incubator"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "wild_egg_hatches"),
                new FunctionGameTestInstance(WILD_EGG_HATCHES.getKey(), isolatedTestData(event, "wild_egg"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "pre_table_hatch_is_owned"),
                new FunctionGameTestInstance(PRE_TABLE_HATCH_IS_OWNED.getKey(), isolatedTestData(event, "pre_table_ownership"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dodo_transports_between_chests"),
                new FunctionGameTestInstance(DODO_TRANSPORTS_BETWEEN_CHESTS.getKey(), isolatedTestData(event, "transport"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "base_upgrades_apply"),
                new FunctionGameTestInstance(BASE_UPGRADES_APPLY.getKey(), isolatedTestData(event, "base_upgrades"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "command_table_uses_one_block"),
                new FunctionGameTestInstance(COMMAND_TABLE_USES_ONE_BLOCK.getKey(), isolatedTestData(event, "command_table_structure"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "store_all_returns_active_dinosaurs"),
                new FunctionGameTestInstance(STORE_ALL_RETURNS_ACTIVE_DINOSAURS.getKey(), isolatedTestData(event, "store_all"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "individual_dinosaur_recall"),
                new FunctionGameTestInstance(INDIVIDUAL_DINOSAUR_RECALL.getKey(), isolatedTestData(event, "single_recall"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "pteranodon_saddle_mounts"),
                new FunctionGameTestInstance(PTERANODON_SADDLE_MOUNTS.getKey(), isolatedTestData(event, "pteranodon_mount"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "defeated_dinosaur_returns_to_depot"),
                new FunctionGameTestInstance(DEFEATED_DINOSAUR_RETURNS_TO_DEPOT.getKey(), isolatedTestData(event, "dinosaur_defeat"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "command_killed_dinosaur_recovers"),
                new FunctionGameTestInstance(COMMAND_KILLED_DINOSAUR_RECOVERS.getKey(), isolatedTestData(event, "command_kill_recovery"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "base_inventory_indexes_chest_contents"),
                new FunctionGameTestInstance(BASE_INVENTORY_INDEXES_CHEST_CONTENTS.getKey(), isolatedTestData(event, "base_inventory"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "stegosaurus_tends_furnace"),
                new FunctionGameTestInstance(STEGOSAURUS_TENDS_FURNACE.getKey(), isolatedTestData(event, "fire_work"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dinosaur_crafts_from_base_storage"),
                new FunctionGameTestInstance(DINOSAUR_CRAFTS_FROM_BASE_STORAGE.getKey(), isolatedTestData(event, "crafting_work"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "crafting_cancels_if_ingredients_change"),
                new FunctionGameTestInstance(CRAFTING_CANCELS_IF_INGREDIENTS_CHANGE.getKey(), isolatedTestData(event, "crafting_transaction"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "expedition_pools_gate_ancient_metal"),
                new FunctionGameTestInstance(EXPEDITION_POOLS_GATE_ANCIENT_METAL.getKey(), isolatedTestData(event, "expedition_rewards"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "food_box_accepts_all_dinosaur_food"),
                new FunctionGameTestInstance(FOOD_BOX_ACCEPTS_ALL_DINOSAUR_FOOD.getKey(), isolatedTestData(event, "food_box"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "hungry_dinosaur_eats_from_food_box"),
                new FunctionGameTestInstance(HUNGRY_DINOSAUR_EATS_FROM_FOOD_BOX.getKey(), isolatedTestData(event, "food_box_feeding"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "turbines_use_full_structure"),
                new FunctionGameTestInstance(TURBINES_USE_FULL_STRUCTURE.getKey(), isolatedTestData(event, "turbine_structure"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "base_energy_stores_and_drains"),
                new FunctionGameTestInstance(BASE_ENERGY_STORES_AND_DRAINS.getKey(), isolatedTestData(event, "base_energy"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "energy_station_rejects_second_worker"),
                new FunctionGameTestInstance(ENERGY_STATION_REJECTS_SECOND_WORKER.getKey(), isolatedTestData(event, "energy_worker_lock"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "powered_piston_requires_base_energy"),
                new FunctionGameTestInstance(POWERED_PISTON_REQUIRES_BASE_ENERGY.getKey(), isolatedTestData(event, "powered_piston"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "powered_interaction_requires_energy"),
                new FunctionGameTestInstance(POWERED_INTERACTION_REQUIRES_ENERGY.getKey(), isolatedTestData(event, "powered_interaction"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "huge_and_albino_modifiers"),
                new FunctionGameTestInstance(HUGE_AND_ALBINO_MODIFIERS.getKey(), isolatedTestData(event, "mutations"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "fossil_restores_albino_appearance"),
                new FunctionGameTestInstance(FOSSIL_RESTORES_ALBINO_APPEARANCE.getKey(), isolatedTestData(event, "fossil_treatment"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "player_kill_permanently_removes_dinosaur"),
                new FunctionGameTestInstance(PLAYER_KILL_PERMANENTLY_REMOVES_DINOSAUR.getKey(), isolatedTestData(event, "permanent_dinosaur_kill"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "processor_compresses_core"),
                new FunctionGameTestInstance(PROCESSOR_COMPRESSES_CORE.getKey(), isolatedTestData(event, "processor_core"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "wind_turbine_upgrade_path"),
                new FunctionGameTestInstance(WIND_TURBINE_UPGRADE_PATH.getKey(),
                        isolatedTestData(event, "wind_turbine_upgrade"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "processor_finishes_without_worker"),
                new FunctionGameTestInstance(PROCESSOR_FINISHES_WITHOUT_WORKER.getKey(),
                        isolatedTestData(event, "processor_natural_cycle"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "processor_transport_round_trip"),
                new FunctionGameTestInstance(PROCESSOR_TRANSPORT_ROUND_TRIP.getKey(),
                        isolatedTestData(event, "processor_transport"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "ancient_furnace_uses_variable_energy"),
                new FunctionGameTestInstance(ANCIENT_FURNACE_USES_VARIABLE_ENERGY.getKey(),
                        isolatedTestData(event, "ancient_furnace"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "roster_health_migrates_attributes"),
                new FunctionGameTestInstance(ROSTER_HEALTH_MIGRATES_ATTRIBUTES.getKey(),
                        isolatedTestData(event, "roster_health"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dinosaurs_breed_same_species"),
                new FunctionGameTestInstance(DINOSAURS_BREED_SAME_SPECIES.getKey(), isolatedTestData(event, "dinosaur_breeding"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "expedition_return_is_one_shot"),
                new FunctionGameTestInstance(EXPEDITION_RETURN_IS_ONE_SHOT.getKey(), isolatedTestData(event, "expedition_return"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "powered_turrets_defend_base"),
                new FunctionGameTestInstance(POWERED_TURRETS_DEFEND_BASE.getKey(), isolatedTestData(event, "powered_turrets"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "expedition_slot_survives_reload"),
                new FunctionGameTestInstance(EXPEDITION_SLOT_SURVIVES_RELOAD.getKey(),
                        isolatedTestData(event, "expedition_roster_reload"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "laser_observer_detects_five_blocks"),
                new FunctionGameTestInstance(LASER_OBSERVER_DETECTS_FIVE_BLOCKS.getKey(),
                        isolatedTestData(event, "laser_observer"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "unmounted_spinosaurus_floats_at_surface"),
                new FunctionGameTestInstance(UNMOUNTED_SPINOSAURUS_FLOATS_AT_SURFACE.getKey(),
                        isolatedTestData(event, "spinosaurus_surface_float"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "heavy_dinosaur_requires_flat_sleep_area"),
                new FunctionGameTestInstance(HEAVY_DINOSAUR_REQUIRES_FLAT_SLEEP_AREA.getKey(),
                        isolatedTestData(event, "heavy_sleep_area"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "hit_worker_resumes_assignment"),
                new FunctionGameTestInstance(HIT_WORKER_RESUMES_ASSIGNMENT.getKey(),
                        isolatedTestData(event, "hit_worker_resume"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "follower_order_survives_reload"),
                new FunctionGameTestInstance(FOLLOWER_ORDER_SURVIVES_RELOAD.getKey(),
                        isolatedTestData(event, "follower_order_reload"))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "follower_quarries_marked_block"),
                new FunctionGameTestInstance(FOLLOWER_QUARRIES_MARKED_BLOCK.getKey(),
                        isolatedTestData(event, "follower_quarry"))
        );
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> isolatedTestData(
            RegisterGameTestsEvent event,
            String name
    ) {
        return isolatedTestData(event, name, 1_200);
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> isolatedTestData(
            RegisterGameTestsEvent event,
            String name,
            int maxTicks
    ) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "test_" + name),
                new TestEnvironmentDefinition.AllOf(List.of())
        );
        return new TestData<>(environment, Identifier.withDefaultNamespace("empty"), maxTicks, 0, true);
    }

    private static void forceTicking(GameTestHelper helper, BlockPos... relativePositions) {
        for (BlockPos relativePos : relativePositions) {
            BlockPos absolutePos = helper.absolutePos(relativePos);
            int centerChunkX = absolutePos.getX() >> 4;
            int centerChunkZ = absolutePos.getZ() >> 4;
            for (int offsetX = -2; offsetX <= 2; offsetX++) {
                for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
                    helper.getLevel().setChunkForced(centerChunkX + offsetX, centerChunkZ + offsetZ, true);
                }
            }
        }
    }

    private static void contentRegistration(GameTestHelper helper) {
        Identifier retiredTurretId = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "magic_turret");
        helper.assertTrue(BuiltInRegistries.BLOCK.get(retiredTurretId).map(Holder::value).orElse(null)
                        == ModBlocks.LASER_TURRET.get(),
                "Saved Magic Turret blocks do not migrate to the Laser Turret");
        helper.assertTrue(BuiltInRegistries.ITEM.get(retiredTurretId).map(Holder::value).orElse(null)
                        == ModItems.LASER_TURRET.get(),
                "Saved Magic Turret items do not migrate to the Laser Turret");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(retiredTurretId).map(Holder::value).orElse(null)
                        == ModBlockEntities.LASER_TURRET.get(),
                "Saved Magic Turret block entities do not migrate to the Laser Turret");
        Identifier retiredRailId = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "enhanced_rail");
        helper.assertTrue(BuiltInRegistries.BLOCK.get(retiredRailId).map(Holder::value).orElse(null)
                        == Blocks.POWERED_RAIL,
                "Saved Enhanced Rail blocks do not migrate to vanilla Powered Rails");
        helper.assertTrue(BuiltInRegistries.ITEM.get(retiredRailId).map(Holder::value).orElse(null)
                        == Items.POWERED_RAIL,
                "Saved Enhanced Rail items do not migrate to vanilla Powered Rails");
        Identifier retiredBayonetId = Identifier.fromNamespaceAndPath(
                PrimevalWorks.MOD_ID, "ancient_reforged_bayonet");
        helper.assertTrue(BuiltInRegistries.ITEM.get(retiredBayonetId).map(Holder::value).orElse(null)
                        == ModItems.PRIMORDIAL_SWORD.get(),
                "Saved Bayonet items do not migrate to the Primordial Sword");
        helper.assertTrue(ModEntities.DINOSAURS.size() == DinosaurSpecies.playableSpecies().size(),
                "The entity registry exposes a dinosaur outside the eight-species jam roster");
        for (String removedSpecies : List.of(
                "brachiosaurus", "dilophosaurus", "ankylosaurus", "pachycephalosaurus")) {
            Identifier entityId = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, removedSpecies);
            Identifier eggId = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, removedSpecies + "_spawn_egg");
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.get(entityId).isEmpty(),
                    removedSpecies + " can still be summoned");
            helper.assertTrue(BuiltInRegistries.ITEM.get(eggId).isEmpty(),
                    removedSpecies + " still has a spawn egg");
        }
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        Block[] blocks = {
                ModBlocks.REINFORCED_PISTON.get(),
                ModBlocks.STICKY_REINFORCED_PISTON.get(),
                ModBlocks.WIND_TURBINE.get(), ModBlocks.UPGRADED_WIND_TURBINE.get(),
                ModBlocks.WATER_TURBINE.get(), ModBlocks.LASER_OBSERVER.get(),
                ModBlocks.ANCIENT_BARREL.get(), ModBlocks.DART_TURRET.get(),
                ModBlocks.PROCESSOR.get(), ModBlocks.ANCIENT_FURNACE.get(),
                ModBlocks.ANCIENT_SPELL_STONE.get(), ModBlocks.LASER_TURRET.get(),
                ModBlocks.SPINOSAURUS_HEAD.get(),
                ModBlocks.PREMIUM_EGG_INCUBATOR.get(),
                ModBlocks.SMALL_DINOSAUR_EGG.get(), ModBlocks.BIG_DINOSAUR_EGG.get(),
                ModBlocks.LARGE_DINOSAUR_EGG.get()
        };
        for (int index = 0; index < blocks.length; index++) {
            BlockPos pos = new BlockPos(index % 4, 1, index / 4);
            helper.setBlock(pos, blocks[index]);
            helper.assertBlockPresent(blocks[index], pos);
        }
        for (int index = 0; index < ModEntities.DINOSAURS.size(); index++) {
            FieldDodoEntity dinosaur = helper.spawn(ModEntities.DINOSAURS.get(index).get(), new BlockPos(4, 1, 4));
            DinosaurSpecies species = DinosaurSpecies.playableSpecies().get(index);
            helper.assertTrue(dinosaur.getType() == ModEntities.DINOSAURS.get(index).get(),
                    "A dinosaur egg resolved to the wrong registered species");
            helper.assertTrue(Math.abs(dinosaur.getBbWidth() - species.collisionWidth() * dinosaur.getGeneticScale()) < 0.001F,
                    species.registryName() + " spawned with the wrong collision width");
            helper.assertTrue(Math.abs(dinosaur.getBbHeight() - species.collisionHeight() * dinosaur.getGeneticScale()) < 0.001F,
                    species.registryName() + " spawned with the wrong collision height");
            helper.assertTrue(dinosaur.getGeneticQuality() >= 0 && dinosaur.getGeneticQuality() <= 100,
                    species.registryName() + " spawned without valid genetics");
            helper.assertTrue(Integer.bitCount(dinosaur.getMutationMask()) <= 2,
                    species.registryName() + " inherited more than two mutations");
            dinosaur.discard();
        }
        helper.assertTrue(DinosaurEggSize.BIG.contains(DinosaurSpecies.STEGOSAURUS),
                "Stegosaurus must hatch from a big dinosaur egg");
        ServerPlayer saddleTester = helper.makeMockServerPlayerInLevel();
        FieldDodoEntity spinosaurus = helper.spawn(ModEntities.SPINOSAURUS.get(), new BlockPos(3, 1, 3));
        DinosaurOwnership.register(saddleTester, spinosaurus);
        saddleTester.getAbilities().instabuild = true;
        saddleTester.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPINOSAURUS_SADDLE.get()));
        saddleTester.interactOn(spinosaurus, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(spinosaurus.isSaddledMount(),
                "The Spinosaurus did not accept the Spinosaurus Saddle replacement");
        saddleTester.interactOn(spinosaurus, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(saddleTester.getVehicle() == spinosaurus,
                "A saddled owned Spinosaurus could not be mounted");
        spinosaurus.setYRot(0.0F);
        Vec3 normalSeat = spinosaurus.getPassengerRidingPosition(saddleTester);
        helper.assertTrue(normalSeat.z > spinosaurus.getZ() + 1.0D,
                "The authored Spinosaurus seat was behind the forward-facing head socket");
        helper.assertTrue(normalSeat.y < spinosaurus.getY() + 68.5D / 16.0D,
                "The rider adjustment no longer lowers the authored Spinosaurus seat");
        helper.assertTrue(spinosaurus.maxUpStep() >= 2.0F,
                "A mounted Spinosaurus cannot traverse its intended two-block terrain step");
        double normalSeatDistance = spinosaurus.getPassengerRidingPosition(saddleTester)
                .distanceTo(spinosaurus.position());
        spinosaurus.setMutationMaskForTesting(FieldDodoEntity.MUTATION_HUGE);
        double hugeSeatDistance = spinosaurus.getPassengerRidingPosition(saddleTester)
                .distanceTo(spinosaurus.position());
        helper.assertTrue(Math.abs(hugeSeatDistance / normalSeatDistance - 1.18D) < 0.002D,
                "The authored Spinosaurus seat did not scale with Huge");
        helper.assertTrue(spinosaurus.skipAttackInteraction(saddleTester),
                "The mounted rider could still strike their own Spinosaurus");
        helper.assertTrue(spinosaurus.requestMountedAttack(saddleTester),
                "The mounted Spinosaurus rejected its server-authoritative attack");
        spinosaurus.discard();
        saddleTester.discard();
        helper.succeed();
    }

    private static void unmountedSpinosaurusFloatsAtSurface(GameTestHelper helper) {
        forceTicking(helper, new BlockPos(4, 1, 4));
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean wall = x == 0 || x == 8 || z == 0 || z == 8;
                for (int y = 1; y <= 6; y++) {
                    helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.STONE : Blocks.WATER);
                }
            }
        }
        FieldDodoEntity spinosaurus = helper.spawn(ModEntities.SPINOSAURUS.get(), new BlockPos(4, 1, 4));
        spinosaurus.setDeltaMovement(0.0D, 0.42D, 0.0D);
        double surfaceY = helper.absolutePos(new BlockPos(4, 7, 4)).getY();
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(spinosaurus.getY() < surfaceY - 1.5D,
                    "The unmounted Spinosaurus jumped most of its body above the water surface");
            helper.assertTrue(spinosaurus.isInWater(),
                    "The unmounted Spinosaurus left the pool; y=" + spinosaurus.getY()
                            + ", velocity=" + spinosaurus.getDeltaMovement()
                            + ", waterDepth=" + spinosaurus.getFluidHeight(net.minecraft.tags.FluidTags.WATER)
                            + ", feetBlock=" + spinosaurus.level().getBlockState(spinosaurus.blockPosition())
                            + ", bounds=" + spinosaurus.getBoundingBox());
            helper.assertTrue(!spinosaurus.isSpinosaurusBreaching(),
                    "Unmounted surface buoyancy incorrectly entered the mounted breach state");
            helper.succeed();
        });
    }

    private static void heavyDinosaurRequiresFlatSleepArea(GameTestHelper helper) {
        BlockPos center = new BlockPos(4, 1, 4);
        for (int x = 3; x <= 5; x++) for (int z = 3; z <= 5; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.AIR);
        }
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.STONE);
        FieldDodoEntity spinosaurus = helper.spawn(ModEntities.SPINOSAURUS.get(), center);
        helper.assertTrue(!spinosaurus.hasSuitableSleepingArea(),
                "A heavyweight dinosaur accepted a one-block sleeping perch");
        for (int x = 3; x <= 5; x++) for (int z = 3; z <= 5; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.assertTrue(spinosaurus.hasSuitableSleepingArea(),
                "A heavyweight dinosaur rejected a flat 3x3 sleeping area");
        spinosaurus.discard();
        helper.succeed();
    }

    private static void hitWorkerResumesAssignment(GameTestHelper helper) {
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(), 1_000L);
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos turbineRelative = new BlockPos(4, 1, 1);
        BlockPos workerRelative = new BlockPos(4, 1, 3);
        forceTicking(helper, tableRelative, turbineRelative, workerRelative);
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 5; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(turbineRelative, ModBlocks.WIND_TURBINE.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        FieldDodoEntity worker = helper.spawn(ModEntities.PARASAUROLOPHUS.get(), workerRelative);
        DinosaurOwnership.register(player, worker);
        BlockPos table = helper.absolutePos(tableRelative);
        BlockPos turbine = helper.absolutePos(turbineRelative);
        worker.assignWork(2, table, List.of(), List.of(turbine), List.of(), null,
                List.of(), List.of(), List.of(), Map.of(turbine, 3),
                0, 3, 1, 0, 0, 0, 0, 1, true, true);

        int[] pulseBeforeHit = {0};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(worker.getWorkAction() == 3,
                        "The test worker never reached its turbine"))
                .thenExecute(() -> {
                    TurbineBlockEntity turbineEntity = helper.getBlockEntity(
                            turbineRelative, TurbineBlockEntity.class);
                    pulseBeforeHit[0] = turbineEntity.getGenerationPulseCount();
                    helper.assertTrue(worker.hurtServer(helper.getLevel(),
                                    helper.getLevel().damageSources().playerAttack(player), 1.0F),
                            "The test hit was not accepted");
                    helper.assertTrue(worker.isWorkEnabled() && worker.getTarget() == null,
                            "A harmless owner hit cancelled or aggroed the assigned worker");
                })
                .thenExecuteAfter(420, () -> {
                    TurbineBlockEntity turbineEntity = helper.getBlockEntity(
                            turbineRelative, TurbineBlockEntity.class);
                    helper.assertTrue(turbineEntity.getGenerationPulseCount() > pulseBeforeHit[0],
                            "The assigned dinosaur never resumed energy work after being hit");
                    player.discard();
                })
                .thenSucceed();
    }

    private static void hugeAndAlbinoModifiers(GameTestHelper helper) {
        FieldDodoEntity dinosaur = helper.spawn(ModEntities.PTERANODON.get(), new BlockPos(2, 1, 2));
        dinosaur.applyIncubatedGenetics(50, 0, 0);
        float baseScale = dinosaur.getGeneticScale();
        double baseHealth = dinosaur.getAttributeValue(Attributes.MAX_HEALTH);
        double baseAttack = dinosaur.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double baseMovement = dinosaur.getAttributeValue(Attributes.MOVEMENT_SPEED);

        dinosaur.setMutationMaskForTesting(FieldDodoEntity.MUTATION_HUGE);
        helper.assertTrue(Math.abs(dinosaur.getGeneticScale() / baseScale - 1.18F) < 0.002F,
                "Huge did not increase real entity scale by 18%");
        helper.assertTrue(Math.abs(dinosaur.getAttributeValue(Attributes.MAX_HEALTH) / baseHealth - 1.20D) < 0.002D,
                "Huge did not increase health by 20%");
        helper.assertTrue(Math.abs(dinosaur.getAttributeValue(Attributes.ATTACK_DAMAGE) / baseAttack - 1.20D) < 0.002D,
                "Huge did not increase damage by 20%");

        dinosaur.setMutationMaskForTesting(FieldDodoEntity.MUTATION_ALBINO);
        helper.assertTrue(Math.abs(dinosaur.getGeneticScale() / baseScale - 1.0F) < 0.002F,
                "Albino incorrectly changed dinosaur size");
        helper.assertTrue(Math.abs(dinosaur.getAttributeValue(Attributes.MAX_HEALTH) / baseHealth - 0.80D) < 0.002D,
                "Albino did not apply its health tradeoff");
        helper.assertTrue(Math.abs(dinosaur.getAttributeValue(Attributes.ATTACK_DAMAGE) / baseAttack - 1.40D) < 0.002D,
                "Albino did not increase damage by 40%");
        helper.assertTrue(Math.abs(dinosaur.getAttributeValue(Attributes.MOVEMENT_SPEED) / baseMovement - 1.40D) < 0.002D,
                "Albino did not increase movement and mount speed by 40%");

        dinosaur.setMutationMaskForTesting(FieldDodoEntity.MUTATION_HUGE | FieldDodoEntity.MUTATION_ALBINO);
        helper.assertTrue(Math.abs(dinosaur.getMutationStatMultiplier() - 1.68F) < 0.002F,
                "Huge and Albino did not stack multiplicatively");
        helper.assertTrue(DinosaurMutationRules.qualityBonus(DinosaurMutationRules.HUGE, 0.0F) == 2
                        && DinosaurMutationRules.qualityBonus(DinosaurMutationRules.HUGE, 0.999F) == 4,
                "A single mutation's birth-quality bias escaped its slight 2-4 point range");
        helper.assertTrue(DinosaurMutationRules.qualityBonus(
                        DinosaurMutationRules.HUGE | DinosaurMutationRules.ALBINO, 0.999F) == 7,
                "The double-mutation quality bias escaped its modest upper bound");
        dinosaur.discard();
        helper.succeed();
    }

    private static void fossilRestoresAlbinoAppearance(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        FieldDodoEntity dinosaur = helper.spawn(ModEntities.TYRANNOSAURUS.get(), new BlockPos(2, 1, 2));
        DinosaurOwnership.register(player, dinosaur);
        dinosaur.setMutationMaskForTesting(FieldDodoEntity.MUTATION_ALBINO);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FOSSIL_FRAGMENT.get()));

        player.interactOn(dinosaur, InteractionHand.MAIN_HAND, Vec3.ZERO);

        helper.assertTrue(dinosaur.hasAlbinoMutation(),
                "Fossil treatment erased the Albino stat mutation");
        helper.assertTrue(!dinosaur.usesAlbinoAppearance() && dinosaur.hasRestoredOriginalPigment(),
                "Fossil treatment did not restore the dinosaur's original colouring");
        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "Fossil treatment did not consume its fragment");
        DinosaurOwnership.OwnedDinosaur stored = DinosaurOwnership.records(player).stream()
                .filter(record -> record.id().equals(dinosaur.getUUID()))
                .findFirst().orElseThrow();
        helper.assertTrue(stored.snapshot().getBooleanOr("PrimevalOriginalPigmentRestored", false),
                "The restored appearance was not persisted to the depot snapshot");
        helper.succeed();
    }

    private static void playerKillPermanentlyRemovesDinosaur(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos dinosaurRelative = new BlockPos(3, 1, 1);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 1), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        CommandTableBlock.claimExisting(player, helper.absolutePos(tableRelative));

        FieldDodoEntity tyrannosaurus = helper.spawn(ModEntities.TYRANNOSAURUS.get(), dinosaurRelative);
        DinosaurOwnership.register(player, tyrannosaurus);
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(
                        player, tyrannosaurus, helper.absolutePos(tableRelative)),
                "The test Tyrannosaurus could not enter the active roster");
        UUID dinosaurId = tyrannosaurus.getUUID();
        tyrannosaurus.hurtServer(
                helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);

        helper.assertTrue(!tyrannosaurus.isAlive(),
                "A dinosaur personally killed by its player remained alive");
        helper.assertTrue(DinosaurOwnership.records(player).stream().noneMatch(record -> record.id().equals(dinosaurId)),
                "A personally killed dinosaur was retained in the depot");
        helper.assertTrue(!DinosaurOwnership.activeIds(player).contains(dinosaurId),
                "A personally killed dinosaur left a stale active-roster slot");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(dinosaurRelative)).inflate(4.0D),
                ItemEntity::isAlive
        );
        helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(ModItems.BIG_DINO_MEAT.get())),
                "A player-killed Tyrannosaurus did not drop big dinosaur meat");
        helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(ModItems.BIG_DINO_BONE.get())),
                "A player-killed Tyrannosaurus did not drop big dinosaur bone");
        helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(ModItems.TYRANNOSAURUS_TOOTH.get())),
                "A player-killed Tyrannosaurus did not drop its trophy tooth");

        BlockPos pteranodonRelative = new BlockPos(5, 1, 1);
        helper.setBlock(new BlockPos(5, 0, 1), Blocks.STONE);
        FieldDodoEntity pteranodon = helper.spawn(ModEntities.PTERANODON.get(), pteranodonRelative);
        DinosaurOwnership.register(player, pteranodon);
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(
                        player, pteranodon, helper.absolutePos(tableRelative)),
                "The test Pteranodon could not enter the active roster");
        UUID pteranodonId = pteranodon.getUUID();
        pteranodon.hurtServer(
                helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
        helper.assertTrue(!pteranodon.isAlive()
                        && DinosaurOwnership.records(player).stream().noneMatch(record -> record.id().equals(pteranodonId)),
                "A player-killed Pteranodon was not permanently removed");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(pteranodonRelative)).inflate(4.0D),
                        ItemEntity::isAlive
                ).stream().anyMatch(item -> item.getItem().is(ModItems.PTERANODON_WING_FRAGMENT.get())),
                "A player-killed Pteranodon did not drop its Wing Fragment");
        player.discard();
        helper.succeed();
    }

    private static void processorCompressesCore(GameTestHelper helper) {
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                1_000L
        );
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos processorRelative = new BlockPos(3, 1, 1);
        BlockPos workerRelative = new BlockPos(3, 1, 2);
        forceTicking(helper, tableRelative, processorRelative, workerRelative);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 2), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(processorRelative, ModBlocks.PROCESSOR.get());
        ProcessorBlockEntity processor = helper.getBlockEntity(processorRelative, ProcessorBlockEntity.class);
        processor.setItem(ProcessorBlockEntity.INPUT_SLOT, new ItemStack(ModItems.CORE.get()));
        processor.setItem(ProcessorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        processor.setItem(ProcessorBlockEntity.CATALYST_SLOT, new ItemStack(ModItems.ANCIENT_METAL_INGOT.get()));
        helper.assertTrue(processor.getItem(ProcessorBlockEntity.OUTPUT_SLOT).isEmpty(),
                "The Processor produced an output before processing began");
        helper.assertTrue(!processor.canPlaceItemThroughFace(ProcessorBlockEntity.OUTPUT_SLOT,
                        new ItemStack(ModItems.CORE.get()), Direction.UP),
                "Automation could insert material into the Processor output");
        processor.toggleAutomationInsert(ProcessorBlockEntity.FUEL_SLOT);
        helper.assertTrue(!processor.canPlaceItemThroughFace(ProcessorBlockEntity.FUEL_SLOT,
                        new ItemStack(Items.COAL), Direction.UP),
                "Disabling fuel insertion did not stop automated refueling");
        processor.toggleAutomationInsert(ProcessorBlockEntity.FUEL_SLOT);

        ProcessorBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(processorRelative),
                helper.getBlockState(processorRelative), processor
        );
        helper.assertTrue(processor.getItem(ProcessorBlockEntity.FUEL_SLOT).is(Items.COAL),
                "The unpowered Processor consumed its fuel");
        helper.assertTrue(!processor.canBeTended(),
                "A dinosaur could tend the Processor while its heavy-power feed was off");
        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        table.receiveGeneratedEnergy(500.0F);
        helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), helper.absolutePos(processorRelative)),
                "The Processor could not connect to the base's heavy-power network");
        helper.assertTrue(processor.canBeTended(),
                "The powered and fully loaded Processor did not become available to workers");
        ProcessorBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(processorRelative),
                helper.getBlockState(processorRelative), processor
        );
        helper.assertTrue(processor.getItem(ProcessorBlockEntity.FUEL_SLOT).isEmpty(),
                "The powered Processor did not convert loaded fuel into active burn time");

        FieldDodoEntity stegosaurus = helper.spawn(ModEntities.STEGOSAURUS.get(), workerRelative);
        BlockPos tablePos = helper.absolutePos(tableRelative);
        BlockPos processorPos = helper.absolutePos(processorRelative);
        stegosaurus.assignWork(1, tablePos, List.of(), List.of(processorPos), List.of(),
                null, List.of(), List.of(), List.of(), Map.of(processorPos, 3),
                0, 3, 1, 0, 0, 0, 0, 1, true, true);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        processor.getItem(ProcessorBlockEntity.OUTPUT_SLOT).is(ModItems.COMPRESSED_CORE.get()),
                        "An assigned Fire worker did not compress the loaded Core; action="
                                + stegosaurus.getWorkAction() + ", progress="
                                + stegosaurus.getWorkActionProgress() + ", sleeping="
                                + stegosaurus.isDinosaurSleeping()
                ))
                .thenExecute(() -> {
                    helper.assertTrue(processor.getItem(ProcessorBlockEntity.INPUT_SLOT).isEmpty(),
                            "Processing produced output without consuming its Core");
                })
                .thenSucceed();
    }

    private static void windTurbineUpgradePath(GameTestHelper helper) {
        var upgrade = ProcessorRecipes.find(
                new ItemStack(ModItems.WIND_TURBINE.get()),
                new ItemStack(ModItems.PTERANODON_WING_FRAGMENT.get())
        ).orElseThrow(() -> new AssertionError("The Wind Turbine upgrade recipe is missing"));
        helper.assertTrue(upgrade.outputStack().is(ModItems.UPGRADED_WIND_TURBINE.get()),
                "The Wind Turbine upgrade produced the wrong item");

        for (int tier = 0; tier <= 2; tier++) {
            int checkedTier = tier;
            helper.assertTrue(ExpeditionRewards.tier(tier).rewards().stream()
                            .noneMatch(reward -> reward.item().get() == ModItems.PTERANODON_WING_FRAGMENT.get()),
                    "Wing Fragments leaked into expedition tier " + checkedTier);
        }
        for (int tier = 3; tier <= 4; tier++) {
            int checkedTier = tier;
            helper.assertTrue(ExpeditionRewards.tier(tier).rewards().stream()
                            .anyMatch(reward -> reward.item().get() == ModItems.PTERANODON_WING_FRAGMENT.get()),
                    "Hard expedition tier " + checkedTier + " cannot award a Wing Fragment");
        }

        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos processorRelative = new BlockPos(3, 1, 1);
        forceTicking(helper, tableRelative, processorRelative);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 1), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(processorRelative, ModBlocks.PROCESSOR.get());
        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        ProcessorBlockEntity processor = helper.getBlockEntity(processorRelative, ProcessorBlockEntity.class);
        processor.setItem(ProcessorBlockEntity.INPUT_SLOT, new ItemStack(ModItems.WIND_TURBINE.get()));
        processor.setItem(ProcessorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        processor.setItem(ProcessorBlockEntity.CATALYST_SLOT,
                new ItemStack(ModItems.PTERANODON_WING_FRAGMENT.get()));
        table.receiveGeneratedEnergy(500.0F);
        helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), helper.absolutePos(processorRelative)),
                "The Processor could not connect for the turbine upgrade");
        ProcessorBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(processorRelative),
                helper.getBlockState(processorRelative), processor
        );
        helper.assertTrue(processor.addWorkerProgress(upgrade.processTicks()),
                "The powered Processor rejected the Wind Turbine upgrade");
        helper.assertTrue(processor.getItem(ProcessorBlockEntity.OUTPUT_SLOT)
                        .is(ModItems.UPGRADED_WIND_TURBINE.get()),
                "The Processor did not finish the upgraded Wind Turbine");

        BlockPos basicRelative = new BlockPos(5, 1, 1);
        BlockPos upgradedRelative = new BlockPos(7, 1, 1);
        helper.setBlock(basicRelative, ModBlocks.WIND_TURBINE.get());
        helper.setBlock(upgradedRelative, ModBlocks.UPGRADED_WIND_TURBINE.get());
        TurbineBlockEntity basic = helper.getBlockEntity(basicRelative, TurbineBlockEntity.class);
        TurbineBlockEntity upgraded = helper.getBlockEntity(upgradedRelative, TurbineBlockEntity.class);
        helper.assertTrue(Math.abs(basic.generationMultiplier() - 0.6F) < 0.0001F,
                "The basic Wind Turbine is not limited to 60% output");
        helper.assertTrue(Math.abs(upgraded.generationMultiplier() - 1.0F) < 0.0001F,
                "The upgraded Wind Turbine is not using the former full output");
        helper.assertTrue(BaseEnergyRules.isGenerator(helper.getBlockState(upgradedRelative)),
                "The energy network does not recognize the upgraded Wind Turbine");
        helper.succeed();
    }

    private static void processorFinishesWithoutWorker(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos processorRelative = new BlockPos(3, 1, 1);
        forceTicking(helper, tableRelative, processorRelative);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 1), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(processorRelative, ModBlocks.PROCESSOR.get());
        ProcessorBlockEntity processor = helper.getBlockEntity(processorRelative, ProcessorBlockEntity.class);
        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        BlockPos processorPos = helper.absolutePos(processorRelative);

        processor.setItem(ProcessorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        table.receiveGeneratedEnergy(500.0F);
        helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), processorPos),
                "The Processor could not connect to the test energy network");
        helper.runAfterDelay(5, () -> helper.assertTrue(
                processor.getItem(ProcessorBlockEntity.FUEL_SLOT).is(Items.COAL),
                "The Processor burned fuel without a valid material and catalyst"));

        helper.runAfterDelay(8, () -> {
            processor.setItem(ProcessorBlockEntity.INPUT_SLOT, new ItemStack(ModItems.CORE.get()));
            processor.setItem(ProcessorBlockEntity.CATALYST_SLOT,
                    new ItemStack(ModItems.ANCIENT_METAL_INGOT.get()));
        });
        helper.startSequence()
                .thenExecuteAfter(10, () -> { })
                .thenWaitUntil(() -> helper.assertTrue(
                        processor.getItem(ProcessorBlockEntity.OUTPUT_SLOT).is(ModItems.COMPRESSED_CORE.get()),
                        "A powered Processor did not finish its recipe without a dinosaur worker"))
                .thenExecute(() -> {
                    helper.assertTrue(processor.getItem(ProcessorBlockEntity.INPUT_SLOT).isEmpty(),
                            "The natural Processor cycle did not consume its material");
                    helper.assertTrue(processor.getItem(ProcessorBlockEntity.CATALYST_SLOT).isEmpty(),
                            "The natural Processor cycle did not consume its catalyst");
                })
                .thenSucceed();
    }

    private static void ancientFurnaceUsesVariableEnergy(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos furnaceRelative = new BlockPos(3, 1, 1);
        forceTicking(helper, tableRelative, furnaceRelative);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 1), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(furnaceRelative, ModBlocks.ANCIENT_FURNACE.get());
        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        AncientFurnaceBlockEntity furnace = helper.getBlockEntity(furnaceRelative, AncientFurnaceBlockEntity.class);
        BlockPos furnacePos = helper.absolutePos(furnaceRelative);

        furnace.setItem(AncientFurnaceBlockEntity.INPUT_SLOT, new ItemStack(Items.RAW_IRON));
        helper.assertTrue(!furnace.canPlaceItem(AncientFurnaceBlockEntity.ENERGY_SLOT, new ItemStack(Items.COAL)),
                "The Ancient Furnace exposed its visual energy socket as a fuel slot");
        helper.assertTrue(Math.abs(BaseEnergyRules.demandPerSecond(helper.getLevel(), furnacePos) - 3.0F) < 0.01F,
                "The authored 20% throttle did not begin at 3 E/S");
        furnace.setThrottle(1.0F);
        helper.assertTrue(Math.abs(BaseEnergyRules.demandPerSecond(helper.getLevel(), furnacePos) - 10.5F) < 0.01F,
                "Moving the Ancient Furnace throttle did not change real network demand");
        furnace.setThrottle(AncientFurnaceBlockEntity.DEFAULT_THROTTLE);

        table.receiveGeneratedEnergy(100.0F);
        helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), furnacePos),
                "The Ancient Furnace could not connect to the base energy network");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        furnace.getItem(AncientFurnaceBlockEntity.OUTPUT_SLOT).is(Items.IRON_INGOT),
                        "The powered Ancient Furnace did not smelt without a fuel item; progress="
                                + furnace.getItem(AncientFurnaceBlockEntity.INPUT_SLOT)
                                + ", storedEnergy=" + table.storedEnergy()))
                .thenExecute(() -> helper.assertTrue(
                        furnace.getItem(AncientFurnaceBlockEntity.ENERGY_SLOT).isEmpty(),
                        "The Ancient Furnace created or retained a hidden fuel stack"))
                .thenSucceed();
    }

    private static void rosterHealthMigratesAttributes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CompoundTag data = player.getPersistentData();
        String prefix = "PrimevalOwnedDinosaur0";
        data.putInt("PrimevalOwnershipSchema", 3);
        data.putInt("PrimevalOwnedDinosaurCount", 1);
        data.putString(prefix + "Id", UUID.randomUUID().toString());
        data.putString(prefix + "Species", DinosaurSpecies.TYRANNOSAURUS.registryName());
        data.putString(prefix + "Name", "Old Save Rex");
        data.putInt(prefix + "Level", 1);
        data.putInt(prefix + "Hunger", 100);
        data.putInt(prefix + "Mood", 100);
        data.putInt(prefix + "Health", 2_000);
        data.putInt(prefix + "MaxHealth", 2_000);
        data.putInt(prefix + "Quality", 50);
        data.putInt(prefix + "Mutations", 0);
        data.putInt(prefix + "Hue", 0);
        DinosaurOwnership.OwnedDinosaur migrated = DinosaurOwnership.records(player).getFirst();
        float expected = FieldDodoEntity.expectedMaxHealth(DinosaurSpecies.TYRANNOSAURUS, 50, 0, 1);
        helper.assertTrue(Math.abs(migrated.maxHealth() - expected) < 0.01F,
                "An old roster kept its obsolete maximum health");
        helper.assertTrue(Math.abs(migrated.health() - migrated.maxHealth()) < 0.01F,
                "A full-health old roster was displayed as partially injured after migration");
        player.discard();
        helper.succeed();
    }

    private static void processorTransportRoundTrip(GameTestHelper helper) {
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                1_000L
        );
        BlockPos sourceRelative = new BlockPos(1, 1, 1);
        BlockPos processorRelative = new BlockPos(5, 1, 1);
        BlockPos destinationRelative = new BlockPos(9, 1, 1);
        BlockPos tableRelative = new BlockPos(5, 1, 4);
        BlockPos dodoRelative = new BlockPos(4, 1, 3);
        forceTicking(helper, sourceRelative, processorRelative, destinationRelative, tableRelative, dodoRelative);
        for (int x = 0; x <= 10; x++) for (int z = 0; z <= 5; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(sourceRelative, Blocks.CHEST);
        helper.setBlock(processorRelative, ModBlocks.PROCESSOR.get());
        helper.setBlock(destinationRelative, Blocks.CHEST);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());

        ChestBlockEntity source = helper.getBlockEntity(sourceRelative, ChestBlockEntity.class);
        ProcessorBlockEntity processor = helper.getBlockEntity(processorRelative, ProcessorBlockEntity.class);
        ChestBlockEntity destination = helper.getBlockEntity(destinationRelative, ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.COAL, 8));
        source.setChanged();

        FieldDodoEntity dodo = helper.spawn(ModEntities.FIELD_DODO.get(), dodoRelative);
        BlockPos tablePos = helper.absolutePos(tableRelative);
        BlockPos sourcePos = helper.absolutePos(sourceRelative);
        BlockPos processorPos = helper.absolutePos(processorRelative);
        BlockPos destinationPos = helper.absolutePos(destinationRelative);
        BaseInventoryIndex.IndexedContainer indexedProcessor = BaseInventoryIndex.scan(
                        helper.getLevel(), tablePos, 50).stream()
                .filter(entry -> entry.pos().equals(processorPos))
                .findFirst().orElseThrow();
        helper.assertTrue(indexedProcessor.canReceiveItems(),
                "An empty Processor was not advertised as a future transport destination");
        helper.assertTrue(indexedProcessor.canSupplyItems(),
                "An empty Processor output could not be pre-wired as a transport source");
        dodo.assignWork(0, tablePos, List.of(sourcePos), List.of(), List.of(processorPos),
                null, List.of(), List.of("minecraft:coal"), List.of(),
                Map.of(sourcePos, 2, processorPos, 3), 0, 2, 8,
                0, 0, 0, 2, 1, true, true);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        processor.getItem(ProcessorBlockEntity.FUEL_SLOT).getCount() == 8,
                        "Transport could not insert fuel into the Processor; source="
                                + source.countItem(Items.COAL) + ", carried=" + dodo.getCarriedStack()
                                + ", action=" + dodo.getWorkAction()
                ))
                .thenExecute(() -> {
                    helper.assertTrue(source.countItem(Items.COAL) == 0,
                            "Processor delivery duplicated or stranded source fuel");
                    processor.setItem(ProcessorBlockEntity.OUTPUT_SLOT,
                            new ItemStack(ModItems.COMPRESSED_CORE.get(), 3));
                    processor.setChanged();
                    dodo.assignWork(0, tablePos, List.of(processorPos), List.of(), List.of(destinationPos),
                            null, List.of(), List.of("primevalworks:compressed_core"), List.of(),
                            Map.of(processorPos, 3, destinationPos, 2), 0, 2, 8,
                            0, 0, 0, 2, 1, true, true);
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        destination.countItem(ModItems.COMPRESSED_CORE.get()) == 3,
                        "Transport could not collect the Processor output; processor="
                                + processor.getItem(ProcessorBlockEntity.OUTPUT_SLOT)
                                + ", carried=" + dodo.getCarriedStack() + ", action=" + dodo.getWorkAction()
                                + ", position=" + dodo.position() + ", destination=" + destinationPos
                                + ", navigationDone=" + dodo.getNavigation().isDone()
                ))
                .thenExecute(() -> helper.assertTrue(
                        processor.getItem(ProcessorBlockEntity.OUTPUT_SLOT).isEmpty(),
                        "Processor output remained after a successful transport collection"
                ))
                .thenSucceed();
    }

    private static void expeditionReturnIsOneShot(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        BlockPos dinosaurRelative = new BlockPos(4, 1, 2);
        forceTicking(helper, tableRelative, dinosaurRelative);
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                1_000L
        );
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 0, 2), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        FieldDodoEntity dinosaur = helper.spawn(ModEntities.FIELD_DODO.get(), dinosaurRelative);
        dinosaur.assignWork(4, helper.absolutePos(tableRelative), List.of(), List.of(), List.of(),
                null, List.of(), List.of(), List.of(), Map.of(),
                1, 1, 1, 0, 0, 0, 0, 1, true, true);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(dinosaur.isOnExpedition(),
                        "The expedition assignment never entered its away state"))
                .thenExecute(() -> {
                    TagValueOutput output = TagValueOutput.createWithContext(
                            ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
                    dinosaur.saveWithoutId(output);
                    CompoundTag snapshot = output.buildResult();
                    snapshot.putLong("PrimevalExpeditionEnd", helper.getLevel().getGameTime() - 1L);
                    dinosaur.discard();

                    FieldDodoEntity restored = ModEntities.FIELD_DODO.get().create(
                            helper.getLevel(), EntitySpawnReason.LOAD);
                    helper.assertTrue(restored != null, "The saved expedition dinosaur could not be recreated");
                    restored.load(TagValueInput.create(
                            ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), snapshot));
                    restored.setPos(helper.absolutePos(dinosaurRelative).getCenter());
                    helper.getLevel().addFreshEntity(restored);
                    restored.reconcilePersistentTimedState();
                    helper.assertTrue(!restored.isOnExpedition(),
                            "An expired expedition remained active after its saved state was loaded");
                    helper.assertTrue(!restored.isWorkEnabled(),
                            "A returned expedition silently relaunched its one-shot work order");
                    helper.assertTrue(!restored.isInvisible() && !restored.isInvulnerable(),
                            "A returned expedition kept its away-state rendering or damage flags");
                })
                .thenSucceed();
    }

    private static void expeditionSlotSurvivesReload(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(3, 1, 3);
        BlockPos awayRelative = new BlockPos(5, 1, 3);
        BlockPos reserveRelative = new BlockPos(6, 1, 3);
        forceTicking(helper, tableRelative, awayRelative, reserveRelative);
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(), 1_000L
        );
        for (int x = 2; x <= 7; x++) helper.setBlock(new BlockPos(x, 0, 3), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        BlockPos tablePos = helper.absolutePos(tableRelative);
        CommandTableBlock.claimExisting(player, tablePos);

        FieldDodoEntity away = helper.spawn(ModEntities.PTERANODON.get(), awayRelative);
        FieldDodoEntity reserve = helper.spawn(ModEntities.PTERANODON.get(), reserveRelative);
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(player, away, tablePos),
                "The expedition Pteranodon could not enter the active crew");
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(player, reserve, tablePos),
                "The reserve Pteranodon could not enter the active crew for setup");
        UUID awayId = away.getUUID();
        UUID reserveId = reserve.getUUID();
        helper.assertTrue(DinosaurOwnership.storeActive(player, reserveId).success(),
                "The second Pteranodon could not enter the depot for setup");

        away.assignWork(4, tablePos, List.of(), List.of(), List.of(), null,
                List.of(), List.of(), List.of(), Map.of(),
                0, 1, 1, 0, 0, 0, 0, 1, true, true);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(away.isOnExpedition(),
                        "The Pteranodon never entered its expedition state"))
                .thenExecute(() -> {
                    DinosaurOwnership.OwnedDinosaur saved = DinosaurOwnership.records(player).stream()
                            .filter(record -> record.id().equals(awayId)).findFirst().orElseThrow();
                    away.discard();
                    FieldDodoEntity reloaded = ModEntities.PTERANODON.get().create(
                            helper.getLevel(), EntitySpawnReason.LOAD);
                    helper.assertTrue(reloaded != null, "The expedition Pteranodon could not reload");
                    reloaded.load(TagValueInput.create(
                            ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved.snapshot()));
                    reloaded.setUUID(awayId);
                    reloaded.setPos(helper.absolutePos(awayRelative).getCenter());
                    helper.assertTrue(helper.getLevel().addFreshEntity(reloaded),
                            "The expedition Pteranodon could not rejoin the loaded world");

                    DinosaurOwnership.activateForTable(player, tablePos, false);
                    DinosaurOwnership.SwapResult swap = DinosaurOwnership.swapIntoActive(
                            player, tablePos, reserveId, 0);
                    helper.assertTrue(!swap.success(),
                            "A depot dinosaur replaced an expedition dinosaur after reload");
                    helper.assertTrue(DinosaurOwnership.activeIds(player).equals(List.of(awayId)),
                            "The failed swap changed the reserved expedition slot");
                    helper.assertTrue(reloaded.isOnExpedition() && reloaded.isInvisible()
                                    && reloaded.isInvulnerable() && reloaded.noPhysics,
                            "Reloading exposed or reactivated the away dinosaur in the base");
                    helper.assertTrue(DinosaurOwnership.findLoaded(player.level().getServer(), reserveId) == null,
                            "The rejected depot Pteranodon still spawned into the base");
                    player.discard();
                })
                .thenSucceed();
    }

    private static void laserObserverDetectsFiveBlocks(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(1, 1, 2);
        BlockPos observerRelative = new BlockPos(3, 1, 2);
        BlockPos fifthRelative = observerRelative.relative(Direction.EAST, 5);
        BlockPos sixthRelative = observerRelative.relative(Direction.EAST, 6);
        BlockPos wallRelative = observerRelative.relative(Direction.EAST, 2);
        for (int x = 1; x <= 10; x++) helper.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(observerRelative, ModBlocks.LASER_OBSERVER.get().defaultBlockState()
                .setValue(PoweredObserverBlock.FACING, Direction.EAST));
        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        BlockPos observerPos = helper.absolutePos(observerRelative);
        table.receiveGeneratedEnergy(50.0F);
        helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), observerPos),
                "The Laser Observer could not connect to base energy");

        helper.startSequence()
                .thenExecuteAfter(6, () -> {
                    helper.assertTrue(!helper.getBlockState(observerRelative)
                                    .getValue(PoweredObserverBlock.POWERED),
                            "The Laser Observer did not settle before the range test");
                    PoweredObserverBlock.notifyDistantObservers(
                            helper.getLevel(), helper.absolutePos(fifthRelative));
                })
                .thenExecuteAfter(3, () -> helper.assertTrue(
                        helper.getBlockState(observerRelative).getValue(PoweredObserverBlock.POWERED),
                        "The Laser Observer missed an update five blocks in front of it"))
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(!helper.getBlockState(observerRelative)
                                    .getValue(PoweredObserverBlock.POWERED),
                            "The Laser Observer pulse did not switch off like a vanilla Observer");
                    PoweredObserverBlock.notifyDistantObservers(
                            helper.getLevel(), helper.absolutePos(sixthRelative));
                })
                .thenExecuteAfter(4, () -> helper.assertTrue(
                        !helper.getBlockState(observerRelative).getValue(PoweredObserverBlock.POWERED),
                        "The Laser Observer detected an update beyond its five-block range"))
                .thenExecute(() -> helper.setBlock(wallRelative, Blocks.STONE))
                .thenExecuteAfter(6, () -> {
                    helper.assertTrue(!helper.getBlockState(observerRelative)
                                    .getValue(PoweredObserverBlock.POWERED),
                            "The Laser Observer did not settle after its beam was blocked");
                    float visibleDistance = BeamLineOfSight.visibleAxisDistance(
                            helper.getLevel(), observerPos, Direction.EAST, 0.505F, 5.50F);
                    helper.assertTrue(visibleDistance < 2.0F,
                            "The Laser Observer beam did not stop against the blocking wall");
                    PoweredObserverBlock.notifyDistantObservers(
                            helper.getLevel(), helper.absolutePos(fifthRelative));
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(!helper.getBlockState(observerRelative)
                                    .getValue(PoweredObserverBlock.POWERED),
                            "The Laser Observer detected an update through a wall");
                    helper.setBlock(wallRelative, Blocks.AIR);
                })
                .thenExecuteAfter(6, () -> {
                    helper.assertTrue(!helper.getBlockState(observerRelative)
                                    .getValue(PoweredObserverBlock.POWERED),
                            "The Laser Observer did not settle after its wall was removed");
                    PoweredObserverBlock.notifyDistantObservers(
                            helper.getLevel(), helper.absolutePos(fifthRelative));
                })
                .thenExecuteAfter(3, () -> helper.assertTrue(
                        helper.getBlockState(observerRelative).getValue(PoweredObserverBlock.POWERED),
                        "The Laser Observer did not reacquire its clear five-block beam"))
                .thenSucceed();
    }

    private static void poweredTurretsDefendBase(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(3, 1, 3);
        BlockPos dartRelative = new BlockPos(5, 1, 2);
        BlockPos laserRelative = new BlockPos(5, 1, 4);
        BlockPos dartThreatRelative = new BlockPos(4, 1, 2);
        BlockPos laserThreatRelative = new BlockPos(2, 1, 4);
        BlockPos laserWallRelative = new BlockPos(4, 1, 4);
        for (int x = 0; x <= 10; x++) for (int z = 0; z <= 6; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(dartRelative, ModBlocks.DART_TURRET.get());
        helper.setBlock(laserRelative, ModBlocks.LASER_TURRET.get());
        helper.setBlock(laserWallRelative, Blocks.STONE);
        helper.setBlock(laserWallRelative.above(), Blocks.STONE);
        DartTurretBlockEntity dartTurret = helper.getBlockEntity(dartRelative, DartTurretBlockEntity.class);
        LaserTurretBlockEntity laserTurret = helper.getBlockEntity(laserRelative, LaserTurretBlockEntity.class);
        dartTurret.setItem(0, new ItemStack(ModItems.DART.get(), 3));
        dartTurret.setChanged();
        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        table.receiveGeneratedEnergy(500.0F);
        helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), helper.absolutePos(dartRelative)),
                "The Dart Turret could not join the powered base network");
        net.minecraft.world.entity.monster.Creeper[] dartThreat = {null};
        float[] dartHealth = {0.0F};
        net.minecraft.world.entity.monster.Creeper[] laserThreat = {null};
        float[] laserHealth = {0.0F};

        helper.startSequence()
                .thenExecute(() -> {
                    helper.assertTrue(BaseEnergyRules.activeDemandPerSecond(
                                    helper.getLevel(), helper.absolutePos(dartRelative)) == 0.0F,
                            "An idle Dart Turret requested base energy without a target");
                    dartThreat[0] = helper.spawn(EntityType.CREEPER, dartThreatRelative);
                    dartThreat[0].setNoAi(true);
                    dartHealth[0] = dartThreat[0].getHealth();
                    helper.assertTrue(BaseEnergyRules.ownsPosition(helper.getLevel(),
                                    helper.absolutePos(dartRelative), dartThreat[0].position()),
                            "The Dart Turret test target was outside its Command Table's base cell");
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        dartTurret.countItem(ModItems.DART.get()) == 2,
                        "The powered Dart Turret did not independently acquire, fire, and consume one dart"))
                .thenExecute(() -> {
                    helper.assertTrue(dartThreat[0].getHealth() < dartHealth[0]
                                    || dartTurret.aimController().targetEntityId() >= 0,
                            "The Dart Turret spent ammunition without retaining a hostile target");
                    dartThreat[0].discard();
                    helper.assertTrue(!table.toggleEnergyConsumer(helper.getLevel(), helper.absolutePos(dartRelative)),
                            "The Dart Turret did not disconnect before the isolated Laser Turret test");
                    helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), helper.absolutePos(laserRelative)),
                            "The Laser Turret could not join the powered base network");
                    BaseEnergyRules.bindConsumer(helper.getLevel(), helper.absolutePos(tableRelative),
                            helper.absolutePos(laserRelative), false);
                    helper.assertTrue(BaseEnergyRules.isPowered(helper.getLevel(), helper.absolutePos(laserRelative)),
                            "A loaded Command Table did not repair a lost runtime energy binding");
                    helper.assertTrue(BaseEnergyRules.activeDemandPerSecond(
                                    helper.getLevel(), helper.absolutePos(laserRelative)) == 0.0F,
                            "An idle Laser Turret requested base energy without a target");
                    laserThreat[0] = helper.spawn(EntityType.CREEPER, laserThreatRelative);
                    laserThreat[0].setNoAi(true);
                    laserHealth[0] = laserThreat[0].getHealth();
                    helper.assertTrue(BaseEnergyRules.ownsPosition(helper.getLevel(),
                                    helper.absolutePos(laserRelative), laserThreat[0].position()),
                            "The Laser Turret test target was outside its Command Table's base cell");
                })
                .thenExecuteAfter(10, () -> {
                    helper.assertTrue(laserTurret.aimController().targetEntityId() < 0,
                            "The Laser Turret acquired a hostile through a wall");
                    helper.assertTrue(Math.abs(laserThreat[0].getHealth() - laserHealth[0]) < 0.01F,
                            "The Laser Turret damaged a hostile through a wall");
                    helper.setBlock(laserWallRelative, Blocks.AIR);
                    helper.setBlock(laserWallRelative.above(), Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        laserTurret.aimController().targetEntityId() >= 0,
                        "The powered Laser Turret did not independently acquire a hostile target; powered="
                                + BaseEnergyRules.isPowered(helper.getLevel(), helper.absolutePos(laserRelative))
                                + ", enabled=" + table.isEnergyConsumerEnabled(helper.absolutePos(laserRelative))
                                + ", stored=" + table.storedEnergy()
                                + ", threatAlive=" + (laserThreat[0] != null && laserThreat[0].isAlive())))
                .thenExecuteAfter(2, () -> {
                    var struck = helper.getLevel().getEntity(laserTurret.aimController().targetEntityId());
                    helper.assertTrue(struck instanceof net.minecraft.world.entity.LivingEntity living
                                    && living.getHealth() < living.getMaxHealth(),
                            "The powered Laser Turret did not independently acquire and strike its target");
                })
                .thenExecuteAfter(18, () -> {
                    helper.assertTrue(!laserThreat[0].isAlive() || laserThreat[0].getHealth() <= 0.0F,
                            "The Laser Turret did not finish its four-hit rapid beam burst");
                    laserThreat[0].discard();
                })
                .thenSucceed();
    }

    private static void dinosaursBreedSameSpecies(GameTestHelper helper) {
        for (int x = 0; x <= 5; x++) for (int z = 0; z <= 4; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        FieldDodoEntity first = helper.spawn(ModEntities.PTERANODON.get(), new BlockPos(2, 1, 2));
        FieldDodoEntity second = helper.spawn(ModEntities.PTERANODON.get(), new BlockPos(4, 1, 2));
        DinosaurOwnership.register(player, first);
        DinosaurOwnership.register(player, second);
        first.setMutationMaskForTesting(DinosaurMutationRules.HUGE);
        second.setMutationMaskForTesting(DinosaurMutationRules.HUGE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NESTING_TREAT.get(), 2));

        player.interactOn(first, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(first.isBreedingPrimed(), "The first Nesting Treat did not prime its dinosaur");
        helper.assertTrue(player.getMainHandItem().getCount() == 1,
                "Priming a dinosaur did not consume exactly one Nesting Treat");
        player.interactOn(second, InteractionHand.MAIN_HAND, Vec3.ZERO);

        ItemStack bredEgg = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (DinosaurEggGenome.read(candidate).isPresent()) {
                bredEgg = candidate;
                break;
            }
        }
        DinosaurEggGenome genome = DinosaurEggGenome.read(bredEgg)
                .orElseThrow(() -> new AssertionError("Breeding did not create a genetic dinosaur egg"));
        helper.assertTrue(genome.species() == DinosaurSpecies.PTERANODON,
                "The bred egg did not preserve its parents' species");
        helper.assertTrue(genome.origin() == DinosaurEggGenome.Origin.BRED,
                "The bred egg lost its breeding origin data");
        helper.assertTrue(genome.quality() >= 4 && genome.quality() <= 100,
                "The bred egg received an invalid quality bonus");
        helper.assertTrue((genome.mutationMask() & ~(DinosaurMutationRules.HUGE | DinosaurMutationRules.ALBINO)) == 0,
                "The bred egg stored an unsupported mutation");
        helper.assertTrue(first.getBreedingCooldownRemaining() > 0L
                        && second.getBreedingCooldownRemaining() > 0L,
                "Successful breeding did not start both parent cooldowns");
        helper.assertTrue(DinosaurMutationRules.rollBred(
                        DinosaurMutationRules.HUGE, DinosaurMutationRules.HUGE, 0.87F, 1.0F)
                        == DinosaurMutationRules.HUGE,
                "Two matching parent mutations did not use their intended inheritance chance");
        first.discard();
        second.discard();
        player.discard();
        helper.succeed();
    }

    private static void foodBoxAcceptsAllDinosaurFood(GameTestHelper helper) {
        BlockPos boxRelative = new BlockPos(2, 1, 2);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(boxRelative, ModBlocks.FOOD_BOX.get());
        FoodBoxBlockEntity foodBox = helper.getBlockEntity(boxRelative, FoodBoxBlockEntity.class);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().setItem(0, new ItemStack(Items.SWEET_BERRIES, 32));
        player.getInventory().setItem(1, new ItemStack(Items.STONE));

        FoodBoxMenu menu = new FoodBoxMenu(1, player.getInventory(), foodBox);
        helper.assertTrue(!menu.getSlot(FoodBoxMenu.FOOD_SLOTS + 28).mayPickup(player),
                "The Food Box let the player grab a non-food inventory item");
        helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),
                "The Food Box accepted a non-food item in its storage row");
        ItemStack moved = menu.quickMoveStack(player, FoodBoxMenu.FOOD_SLOTS + 27);
        helper.assertTrue(moved.is(Items.SWEET_BERRIES) && moved.getCount() == 32,
                "The Food Box did not accept herbivore food through its real menu");
        helper.assertTrue(foodBox.getItem(0).is(Items.SWEET_BERRIES) && foodBox.getItem(0).getCount() == 32,
                "The Food Box menu did not store the transferred food");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(),
                "The transferred food remained duplicated in the player inventory");
        menu.removed(player);
        helper.succeed();
    }

    private static void hungryDinosaurEatsFromFoodBox(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos boxRelative = new BlockPos(3, 1, 1);
        BlockPos dinosaurRelative = new BlockPos(3, 1, 2);
        forceTicking(helper, tableRelative, boxRelative, dinosaurRelative);
        for (int x = 0; x <= 5; x++) for (int z = 0; z <= 3; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(boxRelative, ModBlocks.FOOD_BOX.get());
        FoodBoxBlockEntity foodBox = helper.getBlockEntity(boxRelative, FoodBoxBlockEntity.class);
        foodBox.setItem(0, new ItemStack(Items.SWEET_BERRIES, 3));
        foodBox.setChanged();

        FieldDodoEntity dodo = helper.spawn(ModEntities.FIELD_DODO.get(), dinosaurRelative);
        dodo.feed(-15);
        dodo.assignWork(
                2,
                helper.absolutePos(tableRelative),
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                0, 3, 1, 0, 0, 0, 0, 1, true, true
        );

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        dodo.getHunger() == 100,
                        "The hungry dinosaur did not fill its hunger from the Food Box; hunger=" + dodo.getHunger()
                ))
                .thenExecute(() -> helper.assertTrue(
                        foodBox.getItem(0).getCount() == 1,
                        "Food Box feeding consumed the wrong number of berries: " + foodBox.getItem(0).getCount()
                ))
                .thenSucceed();
    }

    private static void commandTableUsesOneBlock(GameTestHelper helper) {
        BlockPos masterRelative = new BlockPos(2, 1, 2);
        BlockPos extensionRelative = new BlockPos(3, 1, 2);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 2), Blocks.STONE);
        BlockState state = ModBlocks.COMMAND_TABLE.get().defaultBlockState()
                .setValue(CommandTableBlock.FACING, Direction.NORTH);
        helper.setBlock(masterRelative, state);
        BlockPos masterAbsolute = helper.absolutePos(masterRelative);
        helper.assertBlockPresent(ModBlocks.COMMAND_TABLE.get(), masterRelative);
        helper.assertBlockPresent(Blocks.AIR, extensionRelative);
        helper.assertTrue(!state.getCollisionShape(helper.getLevel(), masterAbsolute).isEmpty(),
                "The Command Table needs a full physical hitbox");
        helper.assertTrue(state.getDestroySpeed(helper.getLevel(), masterAbsolute)
                        >= Blocks.OBSIDIAN.defaultBlockState().getDestroySpeed(helper.getLevel(), masterAbsolute),
                "The Command Table must be at least as hard to mine as obsidian");
        BlockState legacyExtension = ModBlocks.COMMAND_TABLE_EXTENSION.get().defaultBlockState();
        helper.setBlock(extensionRelative, legacyExtension);
        helper.assertTrue(legacyExtension.getCollisionShape(helper.getLevel(), helper.absolutePos(extensionRelative)).isEmpty(),
                "A legacy Command Table extension must never keep a second collision box");
        CommandTableBlock.scheduleLegacyCleanup(helper.getLevel(), masterAbsolute);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockPresent(Blocks.AIR, extensionRelative);
            helper.succeed();
        });
    }

    private static void turbinesUseFullStructure(GameTestHelper helper) {
        BlockPos masterRelative = new BlockPos(4, 1, 4);
        BlockPos masterAbsolute = helper.absolutePos(masterRelative);
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.STONE);

        BlockState wind = ModBlocks.WIND_TURBINE.get().defaultBlockState()
                .setValue(TurbineBlock.FACING, Direction.NORTH);
        helper.setBlock(masterRelative, wind);
        ((TurbineBlock) ModBlocks.WIND_TURBINE.get()).assemble(helper.getLevel(), masterAbsolute, wind);
        for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= 2; y++) {
                BlockPos part = masterRelative.offset(x, y, 0);
                helper.assertBlockPresent(ModBlocks.TURBINE_PART.get(), part);
                helper.assertTrue(!helper.getLevel().getBlockState(helper.absolutePos(part))
                                .getCollisionShape(helper.getLevel(), helper.absolutePos(part)).isEmpty(),
                        "Every visible Wind Turbine section needs collision");
            }
        }
        helper.assertBlockPresent(ModBlocks.TURBINE_PART.get(), masterRelative.offset(0, 3, 0));

        helper.setBlock(masterRelative, Blocks.AIR);
        for (int x = -1; x <= 1; x++) {
            helper.assertBlockPresent(Blocks.AIR, masterRelative.offset(x, 1, 0));
            helper.assertBlockPresent(Blocks.AIR, masterRelative.offset(x, 2, 0));
        }
        helper.assertBlockPresent(Blocks.AIR, masterRelative.offset(0, 3, 0));

        helper.setBlock(masterRelative.offset(-1, 0, 0), Blocks.WATER);
        helper.setBlock(masterRelative, Blocks.WATER);
        helper.setBlock(masterRelative.offset(1, 0, 0), Blocks.WATER);
        BlockState water = ModBlocks.WATER_TURBINE.get().defaultBlockState()
                .setValue(TurbineBlock.FACING, Direction.NORTH)
                .setValue(TurbineBlock.WATERLOGGED, true);
        helper.setBlock(masterRelative, water);
        ((TurbineBlock) ModBlocks.WATER_TURBINE.get()).assemble(helper.getLevel(), masterAbsolute, water);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                BlockPos part = masterRelative.offset(x, y, 0);
                helper.assertBlockPresent(ModBlocks.TURBINE_PART.get(), part);
                helper.assertTrue(!helper.getLevel().getBlockState(helper.absolutePos(part))
                                .getCollisionShape(helper.getLevel(), helper.absolutePos(part)).isEmpty(),
                        "Every visible Water Turbine section needs collision");
            }
        }
        TurbineBlockEntity waterTurbine = helper.getBlockEntity(masterRelative, TurbineBlockEntity.class);
        helper.assertTrue(waterTurbine.hasValidEnvironment(),
                "A Water Turbine with its bottom three cells waterlogged did not begin generating");
        BlockPos dryBottomPart = masterRelative.offset(-1, 0, 0);
        BlockState dryState = helper.getBlockState(dryBottomPart)
                .setValue(TurbinePartBlock.WATERLOGGED, false);
        helper.setBlock(dryBottomPart, dryState);
        helper.assertTrue(!waterTurbine.hasValidEnvironment(),
                "A Water Turbine generated while one of its three submerged cells was dry");
        helper.setBlock(dryBottomPart, dryState.setValue(TurbinePartBlock.WATERLOGGED, true));
        helper.assertTrue(waterTurbine.hasValidEnvironment(),
                "Restoring the third waterlogged cell did not restore generation");
        helper.assertTrue(Math.abs(waterTurbine.generationMultiplier() - 1.5F) < 0.001F,
                "Water Turbines must generate exactly 1.5x Wind Turbine output");
        helper.setBlock(masterRelative.offset(1, 0, 0), Blocks.AIR);
        for (int x = -1; x <= 1; x++) {
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(masterRelative.offset(x, 0, 0)))
                            .is(Fluids.WATER),
                    "Breaking a waterlogged turbine must restore its submerged bottom row");
            for (int y = 1; y <= 2; y++) {
                helper.assertBlockPresent(Blocks.AIR, masterRelative.offset(x, y, 0));
            }
        }
        helper.succeed();
    }

    private static void baseEnergyStoresAndDrains(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        BlockPos consumerRelative = new BlockPos(4, 1, 2);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 0, 2), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(consumerRelative, ModBlocks.LASER_OBSERVER.get());
        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        BlockPos consumer = helper.absolutePos(consumerRelative);

        table.receiveGeneratedEnergy(40.0F);
        helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), consumer),
                "The powered block could not join its base energy network");
        helper.assertTrue(table.isEnergyConsumerPowered(consumer),
                "A connected block could not use stored base energy");
        helper.assertTrue(table.enabledEnergyConsumers().contains(consumer),
                "The server did not persist the connected block position");
        BaseEnergyRules.bindConsumer(
                helper.getLevel(), helper.absolutePos(tableRelative), consumer, false
        );
        helper.assertTrue(BaseEnergyRules.isPowered(helper.getLevel(), consumer),
                "A saved energy connection did not immediately rebuild its missing runtime index");

        helper.runAfterDelay(21, () -> {
            helper.assertTrue(table.storedEnergy() < 40.0F && table.storedEnergy() > 38.0F,
                    "The connected block did not drain its 1 E/S demand from storage");
            helper.assertTrue(Math.abs(table.consumptionPerSecond() - 1.0F) < 0.01F,
                    "The server did not report the connected block's energy loss rate");
            helper.assertTrue(!table.toggleEnergyConsumer(helper.getLevel(), consumer),
                    "Clicking an active energy device did not disconnect it");
            helper.assertTrue(!table.isEnergyConsumerEnabled(consumer),
                    "The disconnected block remained attached to the network");
            helper.succeed();
        });
    }

    private static void poweredPistonRequiresBaseEnergy(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(1, 1, 2);
        BlockPos pistonRelative = new BlockPos(4, 1, 2);
        BlockPos redstoneRelative = new BlockPos(3, 1, 2);
        BlockPos obsidianRelative = new BlockPos(5, 1, 2);
        BlockPos stickyRelative = new BlockPos(4, 1, 4);
        BlockPos stickyRedstoneRelative = new BlockPos(3, 1, 4);
        BlockPos stickyObsidianRelative = new BlockPos(5, 1, 4);
        for (int x = 0; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(pistonRelative, ModBlocks.REINFORCED_PISTON.get().defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(obsidianRelative, Blocks.OBSIDIAN);
        helper.setBlock(redstoneRelative, Blocks.REDSTONE_BLOCK);
        helper.setBlock(stickyRelative, ModBlocks.STICKY_REINFORCED_PISTON.get().defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(stickyObsidianRelative, Blocks.CRYING_OBSIDIAN);
        helper.setBlock(stickyRedstoneRelative, Blocks.REDSTONE_BLOCK);
        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        BlockPos piston = helper.absolutePos(pistonRelative);
        BlockPos stickyPiston = helper.absolutePos(stickyRelative);

        helper.startSequence()
                .thenExecuteAfter(4, () -> helper.assertTrue(
                        !helper.getBlockState(pistonRelative).getValue(PistonBaseBlock.EXTENDED),
                        "The Reinforced Piston moved without base energy"
                ))
                .thenExecute(() -> {
                    table.receiveGeneratedEnergy(100.0F);
                    helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), piston),
                            "The Reinforced Piston could not connect to the base network");
                    helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), stickyPiston),
                            "The Sticky Reinforced Piston could not connect to the base network");
                })
                .thenExecuteAfter(4, () -> helper.assertTrue(
                        helper.getBlockState(pistonRelative).getValue(PistonBaseBlock.EXTENDED),
                        "The energized Reinforced Piston did not respond to its redstone input"
                ))
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.OBSIDIAN, obsidianRelative.relative(Direction.EAST));
                    helper.assertBlockPresent(Blocks.CRYING_OBSIDIAN,
                            stickyObsidianRelative.relative(Direction.EAST));
                    helper.setBlock(stickyRedstoneRelative, Blocks.AIR);
                })
                .thenExecuteAfter(5, () -> helper.assertBlockPresent(
                        Blocks.CRYING_OBSIDIAN, stickyObsidianRelative
                ))
                .thenSucceed();
    }

    private static void poweredInteractionRequiresEnergy(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        BlockPos incubatorRelative = new BlockPos(4, 1, 2);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 0, 2), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(incubatorRelative, ModBlocks.PREMIUM_EGG_INCUBATOR.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        CommandTableBlock.claimExisting(player, helper.absolutePos(tableRelative));
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.SMALL_DINOSAUR_EGG.get().getDefaultInstance());

        helper.useBlock(incubatorRelative, player);
        PremiumEggIncubatorBlockEntity incubator = helper.getBlockEntity(
                incubatorRelative, PremiumEggIncubatorBlockEntity.class
        );
        helper.assertTrue(incubator.hasEgg(), "The unpowered Premium Egg Incubator did not accept its egg");
        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "The accepted incubator egg remained in the player's hand");
        PremiumEggIncubatorBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(incubatorRelative),
                helper.getBlockState(incubatorRelative), incubator
        );
        helper.assertTrue(incubator.getProgress() == 0,
                "The unpowered Premium Egg Incubator advanced its timer");

        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        table.receiveGeneratedEnergy(10.0F);
        helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), helper.absolutePos(incubatorRelative)),
                "The incubator could not connect for the powered half of the interaction test");
        helper.startSequence()
                .thenExecuteAfter(3, () -> helper.assertTrue(
                        incubator.getProgress() > 0,
                        "The connected Premium Egg Incubator did not begin incubating"
                ))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    private static void energyStationRejectsSecondWorker(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        BlockPos turbineRelative = new BlockPos(6, 1, 2);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 0, 2), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(turbineRelative, ModBlocks.WIND_TURBINE.get());
        BlockPos table = helper.absolutePos(tableRelative);
        BlockPos turbine = helper.absolutePos(turbineRelative);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        CommandTableBlock.claimExisting(player, table);
        FieldDodoEntity first = helper.spawn(ModEntities.PARASAUROLOPHUS.get(), new BlockPos(3, 1, 2));
        FieldDodoEntity second = helper.spawn(ModEntities.FIELD_DODO.get(), new BlockPos(4, 1, 2));
        DinosaurOwnership.register(player, first);
        DinosaurOwnership.register(player, second);
        DinosaurOwnership.addToActiveIfRoom(player, first, table);
        DinosaurOwnership.addToActiveIfRoom(player, second, table);
        first.assignWork(
                2, table, List.of(), List.of(turbine), List.of(), null, List.of(),
                List.of(), List.of(), Map.of(turbine, 3),
                0, 3, 1, 0, 0, 0, 0, 1, true, true
        );

        helper.assertTrue(DinosaurOwnership.hasEnergyStationAssignment(player, turbine, second.getUUID()),
                "A turbine assigned to one dinosaur remained available to a second dinosaur");
        helper.assertTrue(!DinosaurOwnership.hasEnergyStationAssignment(player, turbine, first.getUUID()),
                "The assignment guard did not exclude the dinosaur editing its own work order");
        helper.succeed();
    }

    private static void storeAllReturnsActiveDinosaurs(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        BlockPos dinosaurRelative = new BlockPos(4, 1, 2);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 0, 2), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        CommandTableBlock.claimExisting(player, helper.absolutePos(tableRelative));

        FieldDodoEntity dinosaur = helper.spawn(ModEntities.FIELD_DODO.get(), dinosaurRelative);
        DinosaurOwnership.register(player, dinosaur);
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(
                        player, dinosaur, helper.absolutePos(tableRelative)),
                "The owned dinosaur could not enter an open active slot");
        BlockPos absoluteTable = helper.absolutePos(tableRelative);
        BlockPos rememberedStation = absoluteTable.offset(2, 0, 1);
        dinosaur.assignWork(
                3,
                absoluteTable,
                List.of(),
                List.of(rememberedStation),
                List.of(),
                null,
                List.of(),
                List.of("minecraft:torch"),
                List.of(),
                Map.of(rememberedStation, 3),
                0, 3, 12, 2, 0, 0, 1, 2, true, false
        );
        dinosaur.awardWorkExperience(17);
        helper.assertTrue(dinosaur.getWorkJobIndex() == 3,
                "The Store All assignment did not reach the dinosaur; value="
                        + dinosaur.getWorkJobIndex());
        UUID dinosaurId = dinosaur.getUUID();

        helper.assertTrue(DinosaurOwnership.storeAllActive(player) == 1,
                "Store All did not return the active dinosaur");
        helper.assertTrue(DinosaurOwnership.activeIds(player).isEmpty(),
                "Store All left an occupied active slot");
        DinosaurOwnership.activateForTable(player, helper.absolutePos(tableRelative), false);
        helper.assertTrue(DinosaurOwnership.activeIds(player).isEmpty(),
                "A normal roster refresh silently reactivated a stored dinosaur");
        helper.assertTrue(dinosaur.isRemoved(),
                "Store All left the active dinosaur in the world");
        DinosaurOwnership.OwnedDinosaur stored = DinosaurOwnership.records(player).stream()
                .filter(record -> record.id().equals(dinosaurId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Store All deleted the dinosaur instead of preserving it in the depot"));
        helper.assertTrue(stored.snapshot().getIntOr("PrimevalWorkJob", -1) == 3,
                "Store All erased the dinosaur's assigned specialty");
        helper.assertTrue(stored.snapshot().getIntOr("PrimevalDinosaurExperience", -1) == 17,
                "Store All erased level progress");
        helper.succeed();
    }

    private static void individualDinosaurRecall(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        for (int x = 0; x <= 10; x++) for (int z = 0; z <= 8; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        BlockPos table = helper.absolutePos(tableRelative);
        CommandTableBlock.claimExisting(player, table);

        FieldDodoEntity recalled = helper.spawn(ModEntities.FIELD_DODO.get(), new BlockPos(8, 1, 6));
        FieldDodoEntity untouched = helper.spawn(ModEntities.FIELD_DODO.get(), new BlockPos(9, 1, 6));
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(player, recalled, table)
                        && DinosaurOwnership.addToActiveIfRoom(player, untouched, table),
                "The individual recall test could not create a two-dinosaur active crew");
        BlockPos workstation = table.offset(3, 0, 2);
        recalled.assignWork(
                3, table, List.of(), List.of(workstation), List.of(), null, List.of(),
                List.of("minecraft:torch"), List.of(), Map.of(workstation, 2),
                0, 3, 8, 0, 0, 0, 0, 1, true, true
        );
        UUID recalledId = recalled.getUUID();
        UUID untouchedId = untouched.getUUID();
        Vec3 untouchedPosition = untouched.position();

        DinosaurOwnership.SwapResult result = DinosaurOwnership.recallActive(player, table, recalledId);
        Vec3 expected = new Vec3(table.getX() + 2.5D, table.getY() + 1.0D, table.getZ() + 0.5D);
        helper.assertTrue(result.success(), "Individual recall was rejected: " + result.message());
        helper.assertTrue(recalled.position().distanceToSqr(expected) < 0.01D,
                "Individual recall did not move the chosen dinosaur to its Command Table slot");
        helper.assertTrue(untouched.position().distanceToSqr(untouchedPosition) < 0.01D,
                "Individual recall moved a different active dinosaur");
        helper.assertTrue(DinosaurOwnership.activeIds(player).equals(List.of(recalledId, untouchedId)),
                "Individual recall changed active crew membership or ordering");
        helper.assertTrue(recalled.isWorkEnabled() && recalled.getWorkJobIndex() == 3
                        && recalled.getWorkWorkstationPositions().equals(List.of(workstation)),
                "Individual recall erased the chosen dinosaur's automation order");
        recalled.discard();
        untouched.discard();
        player.discard();
        helper.succeed();
    }

    private static void pteranodonSaddleMounts(GameTestHelper helper) {
        BlockPos dinosaurRelative = new BlockPos(3, 1, 3);
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        FieldDodoEntity pteranodon = helper.spawn(ModEntities.PTERANODON.get(), dinosaurRelative);
        DinosaurOwnership.register(player, pteranodon);
        player.snapTo(pteranodon.getX(), pteranodon.getY(), pteranodon.getZ() + 1.0D, 180.0F, 0.0F);
        player.getAbilities().instabuild = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PTERANODON_SADDLE.get()));

        player.interactOn(pteranodon, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(pteranodon.isSaddledMount(), "The Pteranodon did not accept its saddle");
        helper.assertTrue(!player.getMainHandItem().isEmpty(),
                "Creative saddle setup did not preserve the held saddle for the mount regression");
        player.interactOn(pteranodon, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getVehicle() == pteranodon,
                "A saddled owned Pteranodon could not be mounted while its saddle remained held");
        var flightTestChunk = pteranodon.chunkPosition();
        helper.getLevel().getChunkSource().addTicketWithRadius(TicketType.FORCED, flightTestChunk, 10);
        boolean[] hoverObserved = {false};
        float[] staminaAtGlideStart = {100.0F};
        helper.onEachTick(() -> {
            if (player.getVehicle() == pteranodon) {
                hoverObserved[0] |= pteranodon.isPteranodonHovering();
                player.setYRot(pteranodon.tickCount < 34 ? 90.0F : 150.0F);
                player.setXRot(pteranodon.tickCount >= 64
                        || pteranodon.tickCount >= 50 && pteranodon.tickCount < 57
                        ? 22.0F : -24.0F);
                boolean takeoff = pteranodon.tickCount >= 5 && pteranodon.tickCount < 13;
                boolean descend = pteranodon.tickCount >= 26 && pteranodon.tickCount < 32;
                boolean forward = pteranodon.tickCount >= 20 && pteranodon.tickCount < 64;
                player.setLastClientInput(new Input(forward, false, false, false, takeoff, false, descend));
            }
        });
        helper.runAfterDelay(4, () -> helper.assertTrue(!pteranodon.isPteranodonAirborne(),
                "The Pteranodon entered flight without an explicit takeoff input"));
        helper.runAfterDelay(18, () -> helper.assertTrue(hoverObserved[0] || pteranodon.isPteranodonHovering(),
                "A Space-only takeoff never settled into the air-idle hover state; airborne="
                        + pteranodon.isPteranodonAirborne() + ", onGround=" + pteranodon.onGround()
                        + ", movement=" + pteranodon.getDeltaMovement() + ", entityTicks=" + pteranodon.tickCount));
        helper.runAfterDelay(31, () -> {
            helper.assertTrue(!pteranodon.isPteranodonGliding(),
                    "Forward-powered flight incorrectly switched to the gliding animation");
            helper.assertTrue(pteranodon.getXRot() > 4.0F,
                    "Sprint/Ctrl input did not pitch the Pteranodon nose-down; pitch="
                            + pteranodon.getXRot());
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(Math.abs(pteranodon.getPteranodonBankDegrees()) > 2.0F,
                    "Looking into a turn did not produce a natural flight bank");
        });
        helper.runAfterDelay(55, () -> helper.assertTrue(pteranodon.isPteranodonGliding(),
                "A high-speed powered dive did not transition into the glide pose"));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(player.getVehicle() == pteranodon,
                    "The rider detached during sustained flight");
            helper.assertTrue(pteranodon.isPteranodonAirborne(),
                    "The mounted Pteranodon never entered its airborne animation state; noGravity="
                            + pteranodon.isNoGravity()
                            + ", onGround=" + pteranodon.onGround()
                            + ", position=" + pteranodon.position()
                            + ", entityTicks=" + pteranodon.tickCount);
            helper.assertTrue(pteranodon.isSprinting(),
                    "Sustained forward input did not reach the Pteranodon's high-speed flight state");
            helper.assertTrue(!pteranodon.isPteranodonGliding(),
                    "The powered dive glide did not return to flapping flight after leveling out");
            helper.assertTrue(Math.abs(Mth.wrapDegrees(pteranodon.getYRot() - 150.0F)) <= 15.0F,
                    "The Pteranodon body did not turn toward the rider's look direction; yaw="
                            + pteranodon.getYRot());
            helper.assertTrue(pteranodon.getPteranodonStamina() < 99.0F,
                    "Powered Pteranodon flight did not consume stamina");
        });
        helper.runAfterDelay(62, () -> {
            pteranodon.setPos(pteranodon.getX(), pteranodon.getY() + 6.0D, pteranodon.getZ());
            pteranodon.setDeltaMovement(Vec3.directionFromRotation(0.0F, pteranodon.getYRot()).scale(0.92D));
        });
        helper.runAfterDelay(64, () -> staminaAtGlideStart[0] = pteranodon.getPteranodonStamina());
        helper.runAfterDelay(72, () -> {
            helper.getLevel().getChunkSource().removeTicketWithRadius(TicketType.FORCED, flightTestChunk, 10);
            helper.assertTrue(pteranodon.isPteranodonGliding(),
                    "Releasing forward input at flying speed did not enter the glide state; airborne="
                            + pteranodon.isPteranodonAirborne()
                            + ", hovering=" + pteranodon.isPteranodonHovering()
                            + ", movement=" + pteranodon.getDeltaMovement()
                            + ", pitch=" + pteranodon.getXRot()
                            + ", entityTicks=" + pteranodon.tickCount);
            helper.assertTrue(pteranodon.getPteranodonAnimationSpeed(1.0F) > 0.72F,
                    "Gliding did not retain a speed-responsive animation cadence");
            helper.assertTrue(pteranodon.getPteranodonStamina() > staminaAtGlideStart[0],
                    "Gliding did not restore Pteranodon stamina");
            player.stopRiding();
            pteranodon.discard();
            player.discard();
            helper.succeed();
        });
    }

    private static void defeatedDinosaurReturnsToDepot(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        BlockPos dinosaurRelative = new BlockPos(4, 1, 2);
        forceTicking(helper, tableRelative, dinosaurRelative);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 0, 2), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        CommandTableBlock.claimExisting(player, helper.absolutePos(tableRelative));

        FieldDodoEntity dinosaur = helper.spawn(ModEntities.FIELD_DODO.get(), dinosaurRelative);
        DinosaurOwnership.register(player, dinosaur);
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(
                        player, dinosaur, helper.absolutePos(tableRelative)),
                "The owned dinosaur could not enter an open active slot");
        BlockPos absoluteTable = helper.absolutePos(tableRelative);
        BlockPos rememberedStation = absoluteTable.offset(2, 0, 1);
        dinosaur.assignWork(
                3,
                absoluteTable,
                List.of(),
                List.of(rememberedStation),
                List.of(),
                null,
                List.of(),
                List.of("minecraft:torch"),
                List.of(),
                Map.of(rememberedStation, 3),
                0, 3, 12, 2, 0, 0, 1, 2, true, false
        );
        dinosaur.awardWorkExperience(17);
        UUID dinosaurId = dinosaur.getUUID();
        dinosaur.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), Float.MAX_VALUE);

        helper.startSequence().thenExecuteAfter(40, () -> {
            helper.assertTrue(dinosaur.isRemoved(), "A defeated owned dinosaur remained active in the world");
            helper.assertTrue(!DinosaurOwnership.activeIds(player).contains(dinosaurId),
                    "A defeated dinosaur left a stale entry in the seven active slots");
            DinosaurOwnership.OwnedDinosaur stored = DinosaurOwnership.records(player).stream()
                    .filter(record -> record.id().equals(dinosaurId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("The defeated dinosaur vanished from the depot"));
            helper.assertTrue(stored.health() >= 1.0F && stored.health() <= stored.maxHealth() * 0.21F,
                    "The depot did not preserve the dinosaur in an injured state");
            helper.assertTrue(stored.snapshot().getIntOr("PrimevalWorkJob", -1) == 3,
                    "Death recovery erased the dinosaur's assigned specialty; saved keys="
                            + stored.snapshot().keySet() + ", value="
                            + stored.snapshot().getIntOr("PrimevalWorkJob", -1));
            helper.assertTrue(stored.snapshot().getBooleanOr("PrimevalWorkEnabled", false),
                    "Death recovery disabled a configured work route");
            helper.assertTrue(stored.snapshot().getIntOr("PrimevalDinosaurExperience", -1) == 17,
                    "Death recovery erased level progress");
            helper.assertTrue(!stored.snapshot().getBooleanOr("PrimevalPendingOwnerRecovery", false)
                            && !stored.snapshot().getBooleanOr("Invulnerable", false),
                    "Recovery stored a transfer-only pending or invulnerable flag");
        }).thenSucceed();
    }

    private static void commandKilledDinosaurRecovers(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        BlockPos dinosaurRelative = new BlockPos(4, 1, 2);
        forceTicking(helper, tableRelative, dinosaurRelative);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 0, 2), Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        BlockPos table = helper.absolutePos(tableRelative);
        CommandTableBlock.claimExisting(player, table);

        FieldDodoEntity spinosaurus = helper.spawn(ModEntities.SPINOSAURUS.get(), dinosaurRelative);
        DinosaurOwnership.register(player, spinosaurus);
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(player, spinosaurus, table),
                "The Spinosaurus could not enter the active crew before the command-kill test");
        UUID id = spinosaurus.getUUID();
        spinosaurus.kill(helper.getLevel());

        helper.startSequence().thenExecuteAfter(40, () -> {
            helper.assertTrue(spinosaurus.isRemoved(),
                    "Command-killed Spinosaurus remained in the world after recall completed; entityTicks="
                            + spinosaurus.tickCount + ", progress="
                            + spinosaurus.getDefeatTransferProgress(0.0F));
            helper.assertTrue(!DinosaurOwnership.activeIds(player).contains(id),
                    "Command-killed Spinosaurus remained in an active crew slot");
            DinosaurOwnership.OwnedDinosaur stored = DinosaurOwnership.records(player).stream()
                    .filter(record -> record.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Command-killed Spinosaurus vanished from ownership"));
            helper.assertTrue(stored.recoveryUntilTick() > helper.getLevel().getGameTime(),
                    "Command-killed Spinosaurus did not enter the recovery row");
        }).thenSucceed();
    }

    private static void baseInventoryIndexesChestContents(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos chestRelative = new BlockPos(4, 1, 1);
        forceTicking(helper, tableRelative, chestRelative);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(chestRelative, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(chestRelative, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.COAL, 23));
        chest.setChanged();

        BlockPos absoluteChest = helper.absolutePos(chestRelative);
        var indexed = BaseInventoryIndex.scan(helper.getLevel(), helper.absolutePos(tableRelative), 50);
        helper.assertTrue(indexed.stream().anyMatch(container -> container.pos().equals(absoluteChest)
                        && container.extractableCount(Identifier.withDefaultNamespace("coal")) == 23),
                "The base inventory index did not expose coal stored in an unopened chest");
        helper.succeed();
    }

    private static void stegosaurusTendsFurnace(GameTestHelper helper) {
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                1_000L
        );
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos furnaceRelative = new BlockPos(3, 1, 1);
        BlockPos dinosaurRelative = new BlockPos(3, 1, 2);
        forceTicking(helper, tableRelative, furnaceRelative, dinosaurRelative);
        for (int x = 0; x <= 5; x++) for (int z = 0; z <= 3; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(furnaceRelative, Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(furnaceRelative, AbstractFurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        furnace.setChanged();

        FieldDodoEntity stegosaurus = helper.spawn(ModEntities.STEGOSAURUS.get(), dinosaurRelative);
        stegosaurus.setMutationMaskForTesting(0);
        stegosaurus.feed(100);
        BlockPos station = helper.absolutePos(furnaceRelative);
        stegosaurus.assignWork(1, helper.absolutePos(tableRelative), List.of(), List.of(station), List.of(),
                null, List.of(), List.of("minecraft:raw_iron"), List.of("minecraft:coal"), Map.of(station, 3),
                0, 3, 1, 0, 0, 0, 0, 1, true, true);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stegosaurus.getWorkAction() == 2
                                && stegosaurus.getWorkActionDuration() == Math.round(
                                        WorkSpecialtyRules.actionDurationTicks(
                                                WorkSpecialtyRules.FIRE_TENDING_TICKS, 3)
                                                / (DinosaurSpecies.STEGOSAURUS.passiveWorkSpeedMultiplier(
                                                        1, stegosaurus.getPassiveStrength())
                                                * DinosaurGeneticPerformanceRules.workSpeedMultiplier(
                                                        stegosaurus.getGeneticQuality()))),
                        "The Stegosaurus never entered its balanced three-star fire work cycle; action="
                                + stegosaurus.getWorkAction() + ", duration=" + stegosaurus.getWorkActionDuration()
                                + ", position=" + stegosaurus.position()
                                + ", sleeping=" + stegosaurus.isDinosaurSleeping()
                                + ", hunger=" + stegosaurus.getHunger()
                                + ", mood=" + stegosaurus.getMood()
                                + ", target=" + (stegosaurus.getTarget() == null
                                        ? "none" : stegosaurus.getTarget().getType().toString())
                                + ", input=" + furnace.getItem(0)
                                + ", fuel=" + furnace.getItem(1)))
                .thenWaitUntil(() -> helper.assertTrue(furnace.countItem(Items.IRON_INGOT) == 1,
                        "Fire work did not finish the loaded furnace; action=" + stegosaurus.getWorkAction()
                                + ", progress=" + stegosaurus.getWorkActionProgress()
                                + "/" + stegosaurus.getWorkActionDuration()))
                .thenSucceed();
    }

    private static void dinosaurCraftsFromBaseStorage(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos chestRelative = new BlockPos(2, 1, 1);
        BlockPos craftingRelative = new BlockPos(4, 1, 1);
        BlockPos dinosaurRelative = new BlockPos(4, 1, 2);
        forceTicking(helper, tableRelative, chestRelative, craftingRelative, dinosaurRelative);
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 3; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(chestRelative, Blocks.CHEST);
        helper.setBlock(craftingRelative, Blocks.CRAFTING_TABLE);
        ChestBlockEntity chest = helper.getBlockEntity(chestRelative, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
        chest.setChanged();

        FieldDodoEntity crafter = helper.spawn(ModEntities.PARASAUROLOPHUS.get(), dinosaurRelative);
        BlockPos station = helper.absolutePos(craftingRelative);
        crafter.assignWork(3, helper.absolutePos(tableRelative), List.of(), List.of(station), List.of(),
                null, List.of(), List.of("minecraft:stick"), List.of(), Map.of(station, 3),
                0, 3, 1, 0, 0, 0, 2, 1, true, true);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crafter.getWorkAction() == 4,
                        "The crafting worker never began the selected recipe"))
                .thenWaitUntil(() -> {
                    int looseSticks = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                                    new net.minecraft.world.phys.AABB(station).inflate(2.0D), ItemEntity::isAlive)
                            .stream().filter(item -> item.getItem().is(Items.STICK))
                            .mapToInt(item -> item.getItem().getCount()).sum();
                    helper.assertTrue(looseSticks == 4,
                            "Crafting did not produce four sticks; output=" + looseSticks
                                    + ", planks=" + chest.countItem(Items.OAK_PLANKS));
                })
                .thenExecute(() -> helper.assertTrue(chest.countItem(Items.OAK_PLANKS) == 0,
                        "Crafting output appeared with " + chest.countItem(Items.OAK_PLANKS)
                                + " planks still in its assigned base storage"))
                .thenSucceed();
    }

    private static void craftingCancelsIfIngredientsChange(GameTestHelper helper) {
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                1_000L
        );
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos chestRelative = new BlockPos(2, 1, 1);
        BlockPos craftingRelative = new BlockPos(4, 1, 1);
        BlockPos dinosaurRelative = new BlockPos(4, 1, 2);
        forceTicking(helper, tableRelative, chestRelative, craftingRelative, dinosaurRelative);
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 3; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(chestRelative, Blocks.CHEST);
        helper.setBlock(craftingRelative, Blocks.CRAFTING_TABLE);
        ChestBlockEntity chest = helper.getBlockEntity(chestRelative, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
        chest.setChanged();

        FieldDodoEntity crafter = helper.spawn(ModEntities.PARASAUROLOPHUS.get(), dinosaurRelative);
        BlockPos station = helper.absolutePos(craftingRelative);
        crafter.assignWork(3, helper.absolutePos(tableRelative), List.of(), List.of(station), List.of(),
                null, List.of(), List.of("minecraft:stick"), List.of(), Map.of(station, 3),
                0, 3, 1, 0, 0, 0, 2, 1, true, true);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crafter.getWorkAction() == 4,
                        "The transactional crafting test never began"))
                .thenExecute(() -> {
                    chest.clearContent();
                    chest.setChanged();
                })
                .thenExecuteAfter(140, () -> {
                    int looseSticks = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                                    new net.minecraft.world.phys.AABB(station).inflate(2.0D), ItemEntity::isAlive)
                            .stream().filter(item -> item.getItem().is(Items.STICK))
                            .mapToInt(item -> item.getItem().getCount()).sum();
                    helper.assertTrue(looseSticks == 0,
                            "Crafting duplicated output after its reserved ingredients disappeared");
                })
                .thenSucceed();
    }

    private static void expeditionPoolsGateAncientMetal(GameTestHelper helper) {
        var ancientMetal = ModItems.RAW_ANCIENT_METAL_INGOT.get();
        for (int tier = 0; tier < 2; tier++) {
            int checkedTier = tier;
            helper.assertTrue(ExpeditionRewards.tier(tier).rewards().stream()
                            .noneMatch(reward -> reward.item().get() == ancientMetal),
                    "Ancient metal leaked into expedition tier " + checkedTier);
        }
        for (int tier = 2; tier < 5; tier++) {
            helper.assertTrue(!ExpeditionRewards.tier(tier).rewards().isEmpty(),
                    "Expedition tier " + tier + " has no authored reward pool");
        }
        helper.assertTrue(ExpeditionRewards.tier(2).rewards().stream()
                        .anyMatch(reward -> reward.item().get() == ancientMetal),
                "Medium expeditions cannot award ancient metal");
        for (var required : List.of(
                ModItems.FOSSIL_FRAGMENT.get(),
                ModItems.SILK.get(),
                ModItems.SULFUR.get(),
                ModItems.DODO_FEATHER.get(),
                ModItems.PTERANODON_WING_FRAGMENT.get(),
                ModItems.TYRANNOSAURUS_TOOTH.get(),
                ModItems.CORE.get(),
                ModItems.NESTING_TREAT.get(),
                ModItems.RAW_ANCIENT_SPELL_INGOT.get(),
                ModItems.COMPRESSED_CORE.get(),
                ModItems.SPINOSAURUS_HEAD.get()
        )) {
            helper.assertTrue(java.util.stream.IntStream.range(0, 5)
                            .mapToObj(ExpeditionRewards::tier)
                            .flatMap(tier -> tier.rewards().stream())
                            .anyMatch(reward -> reward.item().get() == required),
                    "An expedition-only material has no reward source: " + required);
        }
        for (var forbidden : List.of(
                ModItems.SMALL_DINO_BONE.get(),
                ModItems.BIG_DINO_BONE.get(),
                ModItems.SMALL_DINO_MEAT.get(),
                ModItems.BIG_DINO_MEAT.get(),
                ModItems.COMPRESSED_ANCIENT_METAL_INGOT.get()
        )) {
            helper.assertTrue(java.util.stream.IntStream.range(0, 5)
                            .mapToObj(ExpeditionRewards::tier)
                            .flatMap(tier -> tier.rewards().stream())
                            .noneMatch(reward -> reward.item().get() == forbidden),
                    "An expedition bypassed hunting or ore processing with: " + forbidden);
        }
        for (int tier = 0; tier < 4; tier++) {
            int checkedTier = tier;
            helper.assertTrue(ExpeditionRewards.tier(tier).rewards().stream()
                            .noneMatch(reward -> reward.item().get() == ModItems.COMPRESSED_CORE.get()),
                    "Compressed Core leaked into non-rare expedition tier " + checkedTier);
        }
        helper.assertTrue(ExpeditionRewards.tier(4).rewards().stream()
                        .anyMatch(reward -> reward.item().get() == ModItems.COMPRESSED_CORE.get()
                                && reward.weight() == 1),
                "The rarest expedition lacks its very rare Compressed Core jackpot");
        helper.assertTrue(ExpeditionRewards.tier(4).rewards().stream()
                        .anyMatch(reward -> reward.item().get() == ModItems.SPINOSAURUS_HEAD.get()
                                && reward.weight() == 0 && reward.rareChance() > 0.0F),
                "The Primordial Frontier lacks its exceptional Spinosaurus Head find");
        for (var craftedOutput : List.of(
                ModItems.ANCIENT_METAL_INGOT.get(),
                ModItems.ANCIENT_SPELL_INGOT.get(),
                ModItems.COMPRESSED_ANCIENT_METAL_INGOT.get(),
                ModItems.PRIMORDIAL_SWORD.get(),
                ModItems.PTERANODON_SADDLE.get(),
                ModItems.SPINOSAURUS_SADDLE.get()
        )) {
            helper.assertTrue(java.util.stream.IntStream.range(0, 5)
                            .mapToObj(ExpeditionRewards::tier)
                            .flatMap(tier -> tier.rewards().stream())
                            .noneMatch(reward -> reward.item().get() == craftedOutput),
                    "Expeditions bypassed progression by dropping a crafted output: " + craftedOutput);
        }
        helper.succeed();
    }

    private static void baseUpgradesApply(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(2, 1, 2);
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(tablePos, ModBlocks.COMMAND_TABLE.get());
        CommandTableBlockEntity table = helper.getBlockEntity(tablePos, CommandTableBlockEntity.class);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        player.getAbilities().instabuild = false;
        CommandTableBlock.claimExisting(player, helper.absolutePos(tablePos));
        helper.assertTrue(table.isOwnedBy(player.getUUID()), "A fresh Command Table could not be claimed");
        helper.assertTrue(table.baseRadius() == 50, "A new base did not begin with a 50-block radius");
        player.getInventory().add(new ItemStack(Items.COPPER_INGOT, 16));
        player.getInventory().add(new ItemStack(Items.OAK_LOG, 24));

        CommandTableBlockEntity.PurchaseResult radius = table.purchase(player, BaseUpgrade.SURVEY_STAKES.id());
        helper.assertTrue(radius.success(), "Survey Stakes could not be purchased: " + radius.message().getString());
        helper.assertTrue(table.baseRadius() == 54, "Survey Stakes did not extend the base radius by four blocks");

        CommandTableBlockEntity.PurchaseResult speed = table.purchase(player, BaseUpgrade.TRAIL_MARKERS.id());
        helper.assertTrue(speed.success(), "Trail Markers could not be purchased: " + speed.message().getString());
        helper.assertTrue(Math.abs(table.workDurationMultiplier(0) - 0.95F) < 0.001F,
                "Trail Markers did not improve actual work duration");
        helper.assertTrue(player.getInventory().countItem(Items.COPPER_INGOT) == 4,
                "Purchasing two upgrades did not consume their copper cost");
        helper.assertTrue(player.getInventory().countItem(Items.OAK_LOG) == 4,
                "Purchasing two upgrades did not consume their timber cost");

        player.getAbilities().instabuild = true;
        helper.assertTrue(table.purchase(player, BaseUpgrade.WIDE_BOUNDARIES.id()).success(),
                "The radius branch did not unlock after Survey Stakes");
        helper.assertTrue(table.baseRadius() == 62,
                "Wide Boundaries did not add its eight-block radius");
        helper.assertTrue(table.purchase(player, BaseUpgrade.FEEDING_BELLS.id()).success(),
                "The feeding branch did not unlock");
        helper.assertTrue(Math.abs(table.hungerIntervalMultiplier() - 1.08F) < 0.001F,
                "Feeding Bells did not affect real hunger timing");
        helper.assertTrue(table.purchase(player, BaseUpgrade.WORKSHOP_RHYTHM.id()).success(),
                "The workshop branch did not unlock");
        helper.assertTrue(Math.abs(table.workDurationMultiplier(1) - 0.89F) < 0.001F,
                "Workshop Rhythm did not affect real fire-work timing");
        helper.assertTrue(table.purchase(player, BaseUpgrade.COPPER_BUSBARS.id()).success(),
                "The energy branch did not unlock");
        helper.assertTrue(Math.abs(table.workDurationMultiplier(2) - 0.87F) < 0.001F,
                "Copper Busbars did not affect real energy-work timing");
        helper.assertTrue(table.purchase(player, BaseUpgrade.EXPEDITION_CHARTS.id()).success(),
                "The expedition branch did not unlock");
        helper.assertTrue(Math.abs(table.expeditionRewardMultiplier() - 1.10F) < 0.001F,
                "Expedition Charts did not affect actual rewards");
        helper.assertTrue(table.purchase(player, BaseUpgrade.WATCH_POSTS.id()).success()
                        && table.threatAwarenessBonus() == 3,
                "Watch Posts did not affect threat awareness");
        helper.assertTrue(table.purchase(player, BaseUpgrade.QUIET_ROOSTS.id()).success(),
                "The mood branch did not unlock");
        helper.assertTrue(Math.abs(table.moodDrainMultiplier() - 0.91F) < 0.001F,
                "Quiet Roosts did not affect actual mood drain");
        helper.assertTrue(table.purchase(player, BaseUpgrade.CREW_PERCHES.id()).success()
                        && table.activeDinosaurCapacity() == 9,
                "The first crew upgrade did not unlock two real active slots");
        helper.assertTrue(table.followerCapacity() == 1,
                "A fresh base did not begin with exactly one follower slot");

        List<FieldDodoEntity> followers = new java.util.ArrayList<>();
        for (int index = 0; index < 4; index++) {
            BlockPos relative = new BlockPos(4 + index, 1, 2);
            helper.setBlock(relative.below(), Blocks.STONE);
            FieldDodoEntity follower = helper.spawn(ModEntities.FIELD_DODO.get(), relative);
            helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(player, follower, helper.absolutePos(tablePos)),
                    "Follower test companion " + index + " could not join the active crew");
            followers.add(follower);
        }
        helper.assertTrue(DinosaurOwnership.setCommandMode(player, followers.get(0), DinosaurCommandMode.FOLLOW).success(),
                "The first follower slot rejected its companion");
        helper.assertTrue(!DinosaurOwnership.setCommandMode(player, followers.get(1), DinosaurCommandMode.FOLLOW).success()
                        && DinosaurOwnership.followerCount(player) == 1,
                "A fresh base allowed more than one follower");
        helper.assertTrue(table.purchase(player, BaseUpgrade.FIELD_COMMAND.id()).success()
                        && table.purchase(player, BaseUpgrade.FIELD_COMMAND.id()).success()
                        && table.followerCapacity() == 3,
                "Field Command did not unlock the second and third follower slots");
        helper.assertTrue(DinosaurOwnership.setCommandMode(player, followers.get(1), DinosaurCommandMode.FOLLOW).success()
                        && DinosaurOwnership.setCommandMode(player, followers.get(2), DinosaurCommandMode.FOLLOW).success(),
                "Field Command did not admit the second and third followers");
        helper.assertTrue(!DinosaurOwnership.setCommandMode(player, followers.get(3), DinosaurCommandMode.FOLLOW).success()
                        && DinosaurOwnership.followerCount(player) == 3,
                "The upgraded base exceeded its three-follower hard limit");
        followers.forEach(FieldDodoEntity::discard);
        helper.succeed();
    }

    private static void dodoTransportsBetweenChests(GameTestHelper helper) {
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                1_000L
        );
        BlockPos sourceRelative = new BlockPos(1, 1, 1);
        BlockPos destinationRelative = new BlockPos(7, 1, 1);
        BlockPos tableRelative = new BlockPos(4, 1, 4);
        BlockPos dodoRelative = new BlockPos(3, 1, 3);
        forceTicking(helper, sourceRelative, destinationRelative, tableRelative, dodoRelative);
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        helper.setBlock(sourceRelative, Blocks.CHEST);
        helper.setBlock(destinationRelative, Blocks.CHEST);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        ChestBlockEntity source = helper.getBlockEntity(sourceRelative, ChestBlockEntity.class);
        ChestBlockEntity destination = helper.getBlockEntity(destinationRelative, ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.COAL, 24));
        source.setItem(1, new ItemStack(Items.DIRT, 11));
        source.setChanged();

        FieldDodoEntity dodo = helper.spawn(ModEntities.FIELD_DODO.get(), dodoRelative);
        BlockPos sourcePos = helper.absolutePos(sourceRelative);
        BlockPos destinationPos = helper.absolutePos(destinationRelative);
        dodo.assignWork(
                0,
                helper.absolutePos(tableRelative),
                List.of(sourcePos),
                List.of(),
                List.of(destinationPos),
                null,
                List.of(),
                List.of("minecraft:coal"),
                List.of(),
                Map.of(sourcePos, 2, destinationPos, 2),
                0,
                2,
                16,
                0,
                0,
                0,
                0,
                1,
                true,
                true
        );
        assertNeverMovesBackward(helper, dodo, "Transport Dodo");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        destination.countItem(Items.COAL) == 16,
                        "Dodo never completed its chest route; source=" + source.countItem(Items.COAL)
                                + ", destination=" + destination.countItem(Items.COAL)
                                + ", carried=" + dodo.getCarriedStack()
                                + ", position=" + dodo.position()
                                + ", action=" + dodo.getWorkAction()
                                + ", enabled=" + dodo.isWorkEnabled()
                ))
                .thenExecute(() -> {
                    helper.assertTrue(source.countItem(Items.COAL) == 8,
                            "Transport removed the wrong source amount: " + source.countItem(Items.COAL));
                    helper.assertTrue(source.countItem(Items.DIRT) == 11
                                    && destination.countItem(Items.DIRT) == 0,
                            "The selected cargo filter moved an unselected item");
                    helper.assertTrue(dodo.getCarriedStack().isEmpty(),
                            "Dodo kept cargo after completing delivery: " + dodo.getCarriedStack());
                })
                .thenSucceed();
    }

    private static void energyWorkCycle(GameTestHelper helper) {
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                1_000L
        );
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos turbineRelative = new BlockPos(3, 1, 1);
        BlockPos dodoRelative = new BlockPos(3, 1, 2);
        forceTicking(helper, tableRelative, turbineRelative, dodoRelative);
        for (int x = 0; x <= 9; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(turbineRelative, ModBlocks.WIND_TURBINE.get());
        FieldDodoEntity dodo = helper.spawn(ModEntities.FIELD_DODO.get(), dodoRelative);
        BlockPos table = helper.absolutePos(tableRelative);
        BlockPos turbine = helper.absolutePos(turbineRelative);
        dodo.assignWork(
                2,
                table,
                List.of(),
                List.of(turbine),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                Map.of(turbine, 3),
                0,
                3,
                1,
                0,
                0,
                0,
                0,
                1,
                true,
                true
        );
        Vec3[] heldPosition = new Vec3[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        dodo.getWorkAction() == 3,
                        "The energy dino never entered its authored work pose"
                ))
                .thenExecute(() -> {
                    heldPosition[0] = dodo.position();
                    helper.assertBlockEntityData(
                            turbineRelative,
                            TurbineBlockEntity.class,
                            TurbineBlockEntity::isWorkerActive,
                            () -> Component.literal("The turbine rotor did not start with its active worker")
                    );
                })
                .thenExecuteAfter(120, () -> {
                    helper.assertTrue(
                            dodo.position().distanceToSqr(heldPosition[0]) < 0.0001D,
                            "The dino moved while its work animation was supposed to be locked in place"
                    );
                    helper.assertBlockEntityData(
                            turbineRelative,
                            TurbineBlockEntity.class,
                            blockEntity -> blockEntity.getGenerationPulseCount() == 0,
                            () -> net.minecraft.network.chat.Component.literal(
                                    "Energy generation committed before the 1-star work cycle finished"
                            )
                    );
                })
                .thenExecuteAfter(230, () -> {
                    helper.assertBlockEntityData(
                            turbineRelative,
                            TurbineBlockEntity.class,
                            blockEntity -> blockEntity.getGenerationPulseCount() == 1,
                            () -> net.minecraft.network.chat.Component.literal(
                                    "The energy cycle did not produce its generation pulse; action=" + dodo.getWorkAction()
                                            + ", progress=" + dodo.getWorkActionProgress()
                                            + "/" + dodo.getWorkActionDuration()
                                            + ", distance=" + dodo.distanceToSqr(turbine.getCenter())
                                            + ", hunger=" + dodo.getHunger()
                                            + ", entityTicks=" + dodo.tickCount
                            )
                    );
                    helper.assertBlockEntityData(
                            tableRelative,
                            CommandTableBlockEntity.class,
                            tableEntity -> tableEntity.storedEnergy() > 20.0F
                                    && tableEntity.generationPerSecond() > 0.0F,
                            () -> net.minecraft.network.chat.Component.literal(
                                    "The turbine's work never reached the base energy reserve"
                            )
                    );
                })
                .thenExecute(dodo::discard)
                .thenExecuteAfter(8, () -> helper.assertBlockEntityData(
                        turbineRelative,
                        TurbineBlockEntity.class,
                        blockEntity -> !blockEntity.isWorkerActive(),
                        () -> Component.literal("The turbine rotor kept running after its worker left")
                ))
                .thenSucceed();
    }

    private static void energyWorkerStaysOnDuty(GameTestHelper helper) {
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                1_000L
        );
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos turbineRelative = new BlockPos(3, 1, 1);
        BlockPos secondTurbineRelative = new BlockPos(5, 1, 1);
        BlockPos dinosaurRelative = new BlockPos(3, 1, 2);
        forceTicking(helper, tableRelative, turbineRelative, secondTurbineRelative, dinosaurRelative);
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 3; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(turbineRelative, ModBlocks.WIND_TURBINE.get());
        helper.setBlock(secondTurbineRelative, ModBlocks.WIND_TURBINE.get());
        FieldDodoEntity worker = helper.spawn(ModEntities.PARASAUROLOPHUS.get(), dinosaurRelative);
        BlockPos table = helper.absolutePos(tableRelative);
        BlockPos turbine = helper.absolutePos(turbineRelative);
        BlockPos secondTurbine = helper.absolutePos(secondTurbineRelative);
        worker.assignWork(
                2, table, List.of(), List.of(turbine, secondTurbine), List.of(), null, List.of(),
                List.of(), List.of(), Map.of(turbine, 3, secondTurbine, 3),
                0, 3, 1, 0, 0, 0, 0, 1, true, true
        );

        helper.startSequence()
                .thenExecuteAfter(920, () -> {
                    helper.assertBlockEntityData(
                            turbineRelative,
                            TurbineBlockEntity.class,
                            blockEntity -> blockEntity.getGenerationPulseCount() >= 2,
                            () -> Component.literal("The first turbine did not receive its share of a multi-target order")
                    );
                    helper.assertBlockEntityData(
                            secondTurbineRelative,
                            TurbineBlockEntity.class,
                            blockEntity -> blockEntity.getGenerationPulseCount() >= 2,
                            () -> Component.literal("The second turbine was saved but never worked")
                    );
                    helper.assertTrue(worker.isWorkEnabled(),
                            "A continuous energy order disabled itself after repeated cycles");
                    helper.assertTrue(worker.getWorkJobIndex() == 2
                                    && worker.getWorkWorkstationPositions().equals(List.of(turbine, secondTurbine)),
                            "The running energy order lost one of its assigned turbines");
                })
                .thenSucceed();
    }

    private static void workOrderSurvivesRosterRoundTrip(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        BlockPos sourceRelative = new BlockPos(4, 1, 2);
        BlockPos destinationRelative = new BlockPos(5, 1, 2);
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 4; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        BlockPos table = helper.absolutePos(tableRelative);
        BlockPos source = helper.absolutePos(sourceRelative);
        BlockPos destination = helper.absolutePos(destinationRelative);
        CommandTableBlock.claimExisting(player, table);

        FieldDodoEntity original = helper.spawn(ModEntities.FIELD_DODO.get(), new BlockPos(3, 1, 2));
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(player, original, table),
                "The persistence test dinosaur could not join the active crew");
        UUID dinosaurId = original.getUUID();
        original.assignWork(
                0, table, List.of(source), List.of(), List.of(destination), null, List.of(),
                List.of("minecraft:coal"), List.of(), Map.of(source, 3, destination, 2),
                0, 3, 24, 2, 5, 128, 0, 2, true, false
        );

        DinosaurOwnership.activateForTable(player, table, false);
        helper.assertTrue(original.isWorkEnabled(),
                "Refreshing the same Command Table disabled the live work order");
        helper.assertTrue(DinosaurOwnership.records(player).stream()
                        .filter(record -> record.id().equals(dinosaurId))
                        .anyMatch(record -> record.snapshot().getBooleanOr("PrimevalWorkEnabled", false)
                                && record.snapshot().getIntOr("PrimevalWorkJob", -1) == 0),
                "The work order was not written to the ownership snapshot when assigned");

        DinosaurOwnership.SwapResult stored = DinosaurOwnership.storeActive(player, dinosaurId);
        helper.assertTrue(stored.success(), "The active dinosaur could not be stored for a reload test");
        DinosaurOwnership.SwapResult restored = DinosaurOwnership.swapIntoActive(player, table, dinosaurId, 0);
        helper.assertTrue(restored.success(), "The saved dinosaur could not return from its depot snapshot");
        FieldDodoEntity reloaded = DinosaurOwnership.findLoaded(player.level().getServer(), dinosaurId);
        helper.assertTrue(reloaded != null, "The restored dinosaur entity was not present");
        helper.assertTrue(reloaded.isWorkEnabled(), "Depot restore silently disabled the saved work order");
        helper.assertTrue(reloaded.getWorkJobIndex() == 0
                        && reloaded.getWorkSourcePositions().equals(List.of(source))
                        && reloaded.getWorkDestinationPositions().equals(List.of(destination))
                        && reloaded.getWorkItemFilters().equals(List.of("minecraft:coal"))
                        && reloaded.getWorkBatchSize() == 24
                        && reloaded.getWorkSchedule() == 2
                        && reloaded.getWorkSourceReserve() == 5
                        && reloaded.getWorkDestinationTarget() == 128
                        && !reloaded.shouldAvoidDanger(),
                "The restored dinosaur did not retain its complete work configuration");
        reloaded.discard();
        player.discard();
        helper.succeed();
    }

    private static void activeWorkRestoresAfterLogin(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        BlockPos sourceRelative = new BlockPos(4, 1, 2);
        BlockPos destinationRelative = new BlockPos(5, 1, 2);
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 4; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        BlockPos table = helper.absolutePos(tableRelative);
        BlockPos source = helper.absolutePos(sourceRelative);
        BlockPos destination = helper.absolutePos(destinationRelative);
        CommandTableBlock.claimExisting(player, table);

        FieldDodoEntity active = helper.spawn(ModEntities.FIELD_DODO.get(), new BlockPos(3, 1, 2));
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(player, active, table),
                "The login restore test could not create an active crew member");
        UUID activeId = active.getUUID();
        active.assignWork(
                0, table, List.of(source), List.of(), List.of(destination), null, List.of(),
                List.of("minecraft:coal"), List.of(), Map.of(source, 3, destination, 2),
                0, 3, 24, 2, 5, 128, 0, 2, true, false
        );

        FieldDodoEntity reserve = helper.spawn(ModEntities.FIELD_DODO.get(), new BlockPos(6, 1, 2));
        DinosaurOwnership.register(player, reserve);
        UUID reserveId = reserve.getUUID();
        active.discard();
        reserve.discard();

        DinosaurOwnership.restoreActiveForTable(player, table);
        FieldDodoEntity restored = DinosaurOwnership.findLoaded(player.level().getServer(), activeId);
        helper.assertTrue(restored != null, "Login did not restore the saved active dinosaur");
        helper.assertTrue(restored.isWorkEnabled()
                        && restored.getWorkJobIndex() == 0
                        && restored.getWorkSourcePositions().equals(List.of(source))
                        && restored.getWorkDestinationPositions().equals(List.of(destination))
                        && restored.getWorkItemFilters().equals(List.of("minecraft:coal"))
                        && restored.getWorkBatchSize() == 24
                        && restored.getWorkSchedule() == 2
                        && restored.getWorkSourceReserve() == 5
                        && restored.getWorkDestinationTarget() == 128
                        && !restored.shouldAvoidDanger(),
                "Login restored the dinosaur but erased part of its automation order");
        helper.assertTrue(DinosaurOwnership.activeIds(player).equals(List.of(activeId)),
                "Login changed the saved active crew ordering");
        helper.assertTrue(DinosaurOwnership.findLoaded(player.level().getServer(), reserveId) == null,
                "Login incorrectly pulled a depot dinosaur into an empty crew slot");
        restored.discard();
        player.discard();
        helper.succeed();
    }

    private static void hostileTargetsBaseDinosaur(GameTestHelper helper) {
        for (int x = 0; x <= 9; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        BlockPos tableRelative = new BlockPos(1, 1, 2);
        BlockPos dinosaurRelative = new BlockPos(3, 1, 2);
        BlockPos zombieRelative = new BlockPos(8, 1, 2);
        forceTicking(helper, tableRelative, dinosaurRelative, zombieRelative);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        FieldDodoEntity dinosaur = helper.spawn(ModEntities.TYRANNOSAURUS.get(), dinosaurRelative);
        dinosaur.assignWork(
                0,
                helper.absolutePos(tableRelative),
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                0,
                1,
                1,
                0,
                0,
                0,
                0,
                1,
                true,
                true
        );
        var zombie = helper.spawn(EntityType.HUSK, zombieRelative);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        zombie.setHealth(200.0F);
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.getTarget() instanceof FieldDodoEntity,
                    "A hostile mob inside the base did not acquire a dinosaur; target=" + zombie.getTarget()
                            + ", dinoTable=" + dinosaur.getCommandTablePos()
                            + ", alive=" + zombie.isAlive() + "/" + dinosaur.isAlive()
                            + ", canAttack=" + zombie.canAttack(dinosaur)
                            + ", allied=" + zombie.isAlliedTo(dinosaur)
                            + ", distance=" + zombie.distanceTo(dinosaur)
                            + ", dinoTicks=" + dinosaur.tickCount
                            + ", zombieTicks=" + zombie.tickCount);
            helper.assertTrue(dinosaur.getTarget() instanceof Monster hostile && hostile.isAlive(),
                    "A combat-capable dinosaur did not acquire a live hostile inside its base");
            helper.assertTrue(dinosaur.isSprinting(),
                    "A combat-capable dinosaur acquired a hostile mob but did not enter its chase sprint");
            zombie.discard();
            dinosaur.discard();
        });
    }

    private static void tyrannosaurusHuntsFromMouthRange(GameTestHelper helper) {
        for (int x = 0; x <= 18; x++) {
            for (int z = 0; z <= 14; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        BlockPos dinosaurRelative = new BlockPos(3, 1, 7);
        BlockPos zombieRelative = new BlockPos(15, 1, 7);
        forceTicking(helper, dinosaurRelative, zombieRelative);
        FieldDodoEntity dinosaur = helper.spawn(ModEntities.TYRANNOSAURUS.get(), dinosaurRelative);
        var zombie = helper.spawn(EntityType.HUSK, zombieRelative);
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        zombie.setHealth(200.0F);
        helper.onEachTick(() -> {
            if (dinosaur.isAlive() && zombie.isAlive()) dinosaur.setTarget(zombie);
        });
        assertNeverMovesBackward(helper, dinosaur, "Hunting Tyrannosaurus");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        dinosaur.getTarget() == zombie,
                        "The Tyrannosaurus did not acquire its stationary test target; dinoTicks=" + dinosaur.tickCount
                                + ", targetTicks=" + zombie.tickCount
                                + ", dinoAlive=" + dinosaur.isAlive()
                                + ", targetAlive=" + zombie.isAlive()
                                + ", sleeping=" + dinosaur.isDinosaurSleeping()
                                + ", distance=" + dinosaur.distanceTo(zombie)
                ))
                .thenWaitUntil(() -> helper.assertTrue(
                        zombie.getHealth() < 200.0F,
                        "The Tyrannosaurus never landed its animation-timed bite; dino=" + dinosaur.position()
                                + ", target=" + zombie.position()
                                + ", distance=" + dinosaur.distanceTo(zombie)
                                + ", yaw=" + dinosaur.getYRot()
                                + ", bodyYaw=" + dinosaur.yBodyRot
                                + ", targetAlive=" + zombie.isAlive()
                                + ", aggressive=" + dinosaur.isAggressive()
                                + ", sprinting=" + dinosaur.isSprinting()
                                + ", navigationDone=" + dinosaur.getNavigation().isDone()
                ))
                .thenExecuteAfter(30, () -> {
                    double x = zombie.getX() - dinosaur.getX();
                    double z = zombie.getZ() - dinosaur.getZ();
                    double distance = Math.sqrt(x * x + z * z);
                    float targetYaw = (float)(Mth.atan2(z, x) * Mth.RAD_TO_DEG) - 90.0F;
                    float yawError = Math.abs(Mth.wrapDegrees(targetYaw - dinosaur.getYRot()));
                    helper.assertTrue(
                            distance >= 1.75D,
                            "The Tyrannosaurus pushed its body into the target; distance=" + distance
                    );
                    helper.assertTrue(
                            distance <= 4.5D,
                            "The Tyrannosaurus backed outside believable mouth range; distance=" + distance
                                    + ", dino=" + dinosaur.position()
                                    + ", target=" + zombie.position()
                                    + ", yaw=" + dinosaur.getYRot()
                                    + ", targetYaw=" + targetYaw
                                    + ", navigationDone=" + dinosaur.getNavigation().isDone()
                    );
                    helper.assertTrue(
                            yawError <= 42.0F,
                            "The Tyrannosaurus bit without facing the target; yaw error=" + yawError
                    );
                })
                .thenExecute(() -> {
                    zombie.discard();
                    dinosaur.discard();
                })
                .thenSucceed();
    }

    private static void velociraptorWalksWithoutPursuit(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        BlockPos start = new BlockPos(1, 1, 4);
        BlockPos finish = new BlockPos(7, 1, 4);
        forceTicking(helper, start, finish);
        FieldDodoEntity dinosaur = helper.spawn(ModEntities.VELOCIRAPTOR.get(), start);
        dinosaur.setNoAi(true);
        Vec3 origin = dinosaur.position();
        helper.onEachTick(() -> {
            if (dinosaur.isAlive()) {
                dinosaur.setDeltaMovement(0.08D, dinosaur.getDeltaMovement().y, 0.0D);
                dinosaur.setPos(dinosaur.getX() + 0.08D, dinosaur.getY(), dinosaur.getZ());
            }
        });

        helper.startSequence()
                .thenExecuteAfter(50, () -> {
                    helper.assertTrue(dinosaur.position().subtract(origin).horizontalDistance() > 0.75D,
                            "The Velociraptor ordinary-walk check never moved");
                    helper.assertTrue(dinosaur.getRaptorMomentum() <= 0.001F,
                            "Ordinary walking incorrectly built Pursuit momentum");
                    helper.assertTrue(!dinosaur.usesRunAnimation(),
                            "Ordinary walking incorrectly selected the run animation");
                })
                .thenSucceed();
    }

    private static void velociraptorAttackLandsOnContact(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        BlockPos dinosaurRelative = new BlockPos(4, 1, 4);
        BlockPos targetRelative = new BlockPos(5, 1, 4);
        forceTicking(helper, dinosaurRelative, targetRelative);
        FieldDodoEntity dinosaur = helper.spawn(ModEntities.VELOCIRAPTOR.get(), dinosaurRelative);
        var target = helper.spawn(EntityType.HUSK, targetRelative);
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        target.setHealth(200.0F);
        double[] peakHorizontalSpeed = {0.0D};
        helper.onEachTick(() -> {
            if (dinosaur.isAlive() && target.isAlive()) {
                dinosaur.setTarget(target);
                peakHorizontalSpeed[0] = Math.max(
                        peakHorizontalSpeed[0], dinosaur.getDeltaMovement().horizontalDistance());
            }
        });

        helper.startSequence()
                .thenExecuteAfter(4, () -> helper.assertTrue(
                        target.getHealth() == 200.0F,
                        "The Velociraptor dealt damage before the authored bite reached contact"
                ))
                .thenWaitUntil(() -> helper.assertTrue(
                        target.getHealth() < 200.0F,
                        "The Velociraptor attack animation never delivered its delayed hit"
                ))
                .thenExecute(() -> helper.assertTrue(
                        peakHorizontalSpeed[0] > 0.45D,
                        "The Velociraptor stopped for its attack instead of carrying momentum into the pounce"
                ))
                .thenExecute(() -> {
                    target.discard();
                    dinosaur.discard();
                })
                .thenSucceed();
    }

    private static void assertNeverMovesBackward(GameTestHelper helper, FieldDodoEntity dinosaur, String label) {
        helper.onEachTick(() -> {
            Vec3 movement = dinosaur.getDeltaMovement();
            double speed = movement.horizontalDistance();
            if (speed < 0.035D || dinosaur.getWorkAction() != 0) {
                return;
            }
            double yaw = dinosaur.yBodyRot * Mth.DEG_TO_RAD;
            double facingX = -Math.sin(yaw);
            double facingZ = Math.cos(yaw);
            double alignment = (facingX * movement.x + facingZ * movement.z) / speed;
            helper.assertTrue(
                    alignment >= 0.12D,
                    label + " moved backward; alignment=" + alignment
                            + ", movement=" + movement
                            + ", yaw=" + dinosaur.getYRot()
                            + ", bodyYaw=" + dinosaur.yBodyRot
            );
        });
    }

    private static void incubatorImprovesHatchling(GameTestHelper helper) {
        BlockPos incubatorRelative = new BlockPos(4, 1, 2);
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        forceTicking(helper, incubatorRelative, tableRelative);
        BlockPos absolute = helper.absolutePos(incubatorRelative);
        helper.getLevel().getEntitiesOfClass(
                FieldDodoEntity.class,
                new net.minecraft.world.phys.AABB(absolute).inflate(6.0D)
        ).forEach(FieldDodoEntity::discard);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(incubatorRelative, ModBlocks.PREMIUM_EGG_INCUBATOR.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        CommandTableBlock.claimExisting(player, helper.absolutePos(tableRelative));
        CommandTableBlockEntity table = helper.getBlockEntity(tableRelative, CommandTableBlockEntity.class);
        BlockPos incubatorPos = helper.absolutePos(incubatorRelative);
        table.receiveGeneratedEnergy(20.0F);
        helper.assertTrue(table.toggleEnergyConsumer(helper.getLevel(), incubatorPos),
                "The incubator could not connect to the base energy network");
        helper.onEachTick(() -> table.receiveGeneratedEnergy(3.0F / 20.0F));
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.SMALL_DINOSAUR_EGG.get().getDefaultInstance());
        helper.useBlock(incubatorRelative, player);
        PremiumEggIncubatorBlockEntity incubator = helper.getBlockEntity(
                incubatorRelative,
                PremiumEggIncubatorBlockEntity.class
        );
        helper.assertTrue(incubator.hasEgg(), "Right-clicking the incubator did not insert the egg");
        helper.assertTrue(incubator.getGeneticQuality() >= 62,
                "Premium incubation rolled quality " + incubator.getGeneticQuality() + " instead of 62+");
        helper.assertTrue(Integer.bitCount(incubator.getMutationMask()) <= 2,
                "Premium incubation rolled more than two mutations");
        helper.assertTrue(smallEggSpecies(incubator.getSelectedSpecies()),
                "A small incubated egg selected the wrong size class: " + incubator.getSelectedSpecies());
        DinosaurEggGenome lockedGenome = DinosaurEggGenome.read(incubator.getEgg())
                .orElseThrow(() -> new AssertionError("The incubator did not write its locked genome to the egg"));
        ItemStack removedEgg = incubator.removeEgg();
        helper.assertTrue(!incubator.hasEgg(), "Removing an incubated egg did not clear the machine");
        player.setItemInHand(InteractionHand.MAIN_HAND, removedEgg);
        helper.useBlock(incubatorRelative, player);
        DinosaurEggGenome reinsertedGenome = DinosaurEggGenome.read(incubator.getEgg())
                .orElseThrow(() -> new AssertionError("The reinserted egg lost its locked genome"));
        helper.assertTrue(lockedGenome.equals(reinsertedGenome),
                "Removing and reinserting an incubated egg rerolled its genetics");
        helper.startSequence()
                .thenExecuteAfter(incubator.getRequiredTicks() + 40, () -> {
                    helper.assertTrue(!incubator.hasEgg(), "The incubator did not consume its finished egg");
                })
                .thenSucceed();
    }

    private static void wildEggHatches(GameTestHelper helper) {
        BlockPos eggRelative = new BlockPos(4, 1, 2);
        BlockPos tableRelative = new BlockPos(2, 1, 2);
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        forceTicking(helper, eggRelative, tableRelative);
        BlockPos absolute = helper.absolutePos(eggRelative);
        helper.getLevel().getEntitiesOfClass(
                FieldDodoEntity.class,
                new net.minecraft.world.phys.AABB(absolute).inflate(6.0D)
        ).forEach(FieldDodoEntity::discard);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(eggRelative, ModBlocks.SMALL_DINOSAUR_EGG.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        CommandTableBlock.claimExisting(player, helper.absolutePos(tableRelative));
        helper.useBlock(eggRelative, player);
        helper.assertBlockNotPresent(ModBlocks.SMALL_DINOSAUR_EGG.get(), eggRelative);
        List<FieldDodoEntity> hatchlings = helper.getLevel().getEntitiesOfClass(
                FieldDodoEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(tableRelative)).inflate(12.0D),
                dino -> dino.getCommandTablePos().filter(helper.absolutePos(tableRelative)::equals).isPresent()
        );
        helper.assertTrue(hatchlings.size() == 1,
                "A wild egg should immediately add exactly one linked dinosaur; found " + hatchlings.size());
        helper.assertTrue(hatchlings.getFirst().getGeneticQuality() >= 0,
                "A wild hatchling did not initialize its genetics");
        helper.assertTrue(smallEggSpecies(hatchlings.getFirst().getSpecies()),
                "A small wild egg hatched the wrong size class: " + hatchlings.getFirst().getSpecies());
        helper.assertTrue(hatchlings.getFirst().isOwnedBy(player.getUUID()),
                "The wild hatchling was not owned by the player who hatched it");
        helper.assertTrue(DinosaurOwnership.records(player).stream()
                        .anyMatch(record -> record.id().equals(hatchlings.getFirst().getUUID())),
                "The wild hatchling was not written to the persistent dinosaur depot");
        List<ItemEntity> fragments = helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class,
                        new net.minecraft.world.phys.AABB(absolute).inflate(2.0D),
                        item -> item.getItem().is(ModItems.FOSSIL_FRAGMENT.get())
                );
        helper.assertTrue(fragments.size() == 1
                        && fragments.getFirst().getItem().getCount() >= 1
                        && fragments.getFirst().getItem().getCount() <= 3,
                "A wild egg did not leave a single 1-3 Fossil Fragment reward stack");
        helper.succeed();
    }

    private static void preTableHatchIsOwned(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        BlockPos tableRelative = new BlockPos(4, 1, 4);
        forceTicking(helper, tableRelative);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        BlockPos playerPos = helper.absolutePos(tableRelative);
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

        DinosaurHatching.HatchResult result = DinosaurHatching.hatchWildEgg(player, DinosaurEggSize.SMALL);
        helper.assertTrue(result.success() && result.dinosaur() != null,
                "A dinosaur egg could not hatch before the player placed a Command Table");
        FieldDodoEntity dinosaur = result.dinosaur();
        helper.assertTrue(dinosaur.isOwnedBy(player.getUUID()), "The pre-table hatchling has no owner");
        helper.assertTrue(DinosaurOwnership.records(player).stream()
                        .anyMatch(record -> record.id().equals(dinosaur.getUUID())),
                "The pre-table hatchling was not saved in the player's depot");
        helper.assertTrue(DinosaurOwnership.activeIds(player).isEmpty(),
                "A pre-table hatchling became active without a base");

        for (int index = 1; index < 8; index++) {
            DinosaurHatching.HatchResult extra = DinosaurHatching.hatchWildEgg(player, DinosaurEggSize.SMALL);
            helper.assertTrue(extra.success(), "Pre-table hatch " + (index + 1) + " was not saved");
        }
        helper.assertTrue(DinosaurOwnership.records(player).size() == 8,
                "All eight pre-table hatchlings were not saved to the player's ownership record");

        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        CommandTableBlock.claimExisting(player, helper.absolutePos(tableRelative));
        helper.assertTrue(DinosaurOwnership.activeIds(player).size() == DinosaurOwnership.STARTING_ACTIVE_LIMIT,
                "The first Command Table did not fill exactly seven active crew slots");
        helper.assertTrue(DinosaurOwnership.activeIds(player).contains(dinosaur.getUUID()),
                "The saved hatchling did not join the first Command Table crew");
        helper.assertTrue(dinosaur.getCommandTablePos().filter(helper.absolutePos(tableRelative)::equals).isPresent(),
                "The saved hatchling was not linked to the first Command Table");
        UUID depotId = DinosaurOwnership.records(player).stream()
                .map(DinosaurOwnership.OwnedDinosaur::id)
                .filter(id -> !DinosaurOwnership.activeIds(player).contains(id))
                .findFirst().orElseThrow();
        helper.assertTrue(DinosaurOwnership.findLoaded(player.level().getServer(), depotId) == null,
                "The eighth hatchling remained in the world instead of entering the depot");
        helper.succeed();
    }

    private static boolean smallEggSpecies(DinosaurSpecies species) {
        return species == DinosaurSpecies.DODO
                || species == DinosaurSpecies.VELOCIRAPTOR
                || species == DinosaurSpecies.PTERANODON;
    }

    private static void spinosaurusClearsCloseTarget(GameTestHelper helper) {
        for (int x = 0; x <= 9; x++) for (int z = 0; z <= 8; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        BlockPos tableRelative = new BlockPos(1, 1, 4);
        BlockPos dinosaurRelative = new BlockPos(4, 1, 4);
        BlockPos threatRelative = new BlockPos(4, 1, 5);
        forceTicking(helper, tableRelative, dinosaurRelative, threatRelative);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        FieldDodoEntity spinosaurus = helper.spawn(ModEntities.SPINOSAURUS.get(), dinosaurRelative);
        spinosaurus.assignWork(
                0, helper.absolutePos(tableRelative), List.of(), List.of(), List.of(), null,
                List.of(), List.of(), List.of(), Map.of(),
                0, 1, 1, 0, 0, 0, 0, 1, true, true
        );
        var threat = helper.spawn(EntityType.HUSK, threatRelative);
        threat.setNoAi(true);
        threat.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
        threat.setHealth(200.0F);
        Vec3 initialThreatPosition = threat.position();
        helper.onEachTick(() -> {
            if (spinosaurus.isAlive() && threat.isAlive()) spinosaurus.setTarget(threat);
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        threat.getHealth() < 200.0F,
                        "The Spinosaurus still refused to bite a target inside its ideal mouth spacing"))
                .thenExecuteAfter(3, () -> helper.assertTrue(
                        threat.position().subtract(initialThreatPosition).horizontalDistanceSqr() > 0.01D
                                || threat.getDeltaMovement().horizontalDistanceSqr() > 0.01D,
                        "The Spinosaurus did not apply a clearing impulse to a crowding target"))
                .thenExecute(() -> {
                    threat.discard();
                    spinosaurus.discard();
                })
                .thenSucceed();
    }

    private static void followerOrderSurvivesReload(GameTestHelper helper) {
        BlockPos dinosaurRelative = new BlockPos(2, 1, 2);
        BlockPos first = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos second = helper.absolutePos(new BlockPos(7, 3, 7));
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        FieldDodoEntity original = helper.spawn(ModEntities.FIELD_DODO.get(), dinosaurRelative);
        original.setCommandMode(DinosaurCommandMode.FOLLOW);
        original.assignFieldWork(new DinoWhistleSettings(
                DinoWhistleSettings.FieldMode.HARVEST,
                DinoWhistleSettings.Pattern.AREA,
                true,
                77
        ), first, second);

        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.saveWithoutId(output);
        CompoundTag snapshot = output.buildResult();
        original.discard();

        FieldDodoEntity restored = ModEntities.FIELD_DODO.get().create(
                helper.getLevel(), EntitySpawnReason.LOAD);
        helper.assertTrue(restored != null, "The follower could not be recreated from its saved state");
        restored.load(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), snapshot));
        helper.assertTrue(restored.getCommandMode() == DinosaurCommandMode.FOLLOW,
                "The saved follower reverted to Home after reload");
        helper.assertTrue(restored.hasFieldWork()
                        && restored.getFieldWorkMode() == DinoWhistleSettings.FieldMode.HARVEST,
                "The follower lost its field-work mode after reload");
        helper.assertTrue(restored.getFieldWorkPattern() == DinoWhistleSettings.Pattern.AREA
                        && restored.isFieldWorkContinuous(),
                "The follower lost its area or continuous settings after reload");
        helper.assertTrue(restored.getFieldWorkRange() == 77
                        && restored.getFieldWorkFirst().filter(first::equals).isPresent()
                        && restored.getFieldWorkSecond().filter(second::equals).isPresent(),
                "The follower lost its leash or selected corners after reload");
        helper.succeed();
    }

    private static void followerQuarriesMarkedBlock(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(1, 1, 1);
        BlockPos dinosaurRelative = new BlockPos(3, 1, 3);
        BlockPos targetRelative = new BlockPos(5, 1, 3);
        forceTicking(helper, tableRelative, dinosaurRelative, targetRelative);
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 6; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.setBlock(targetRelative, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_POS);
        player.getPersistentData().remove(CommandTableBlock.OWNER_TABLE_DIMENSION);
        player.snapTo(helper.absolutePos(new BlockPos(2, 1, 3)).getCenter().x,
                helper.absolutePos(new BlockPos(2, 1, 3)).getY(),
                helper.absolutePos(new BlockPos(2, 1, 3)).getCenter().z, 0.0F, 0.0F);
        BlockPos table = helper.absolutePos(tableRelative);
        BlockPos target = helper.absolutePos(targetRelative);
        CommandTableBlock.claimExisting(player, table);
        FieldDodoEntity nonspecialist = helper.spawn(ModEntities.STEGOSAURUS.get(), new BlockPos(3, 1, 5));
        nonspecialist.setCommandMode(DinosaurCommandMode.FOLLOW);
        nonspecialist.assignFieldWork(new DinoWhistleSettings(
                DinoWhistleSettings.FieldMode.COLLECT,
                DinoWhistleSettings.Pattern.SINGLE,
                false,
                48
        ), target, null);
        helper.assertTrue(!nonspecialist.hasFieldWork(),
                "A species without a field specialty accepted a whistle order");
        nonspecialist.discard();
        FieldDodoEntity dinosaur = helper.spawn(ModEntities.TYRANNOSAURUS.get(), dinosaurRelative);
        helper.assertTrue(DinosaurOwnership.addToActiveIfRoom(player, dinosaur, table),
                "The quarry test companion could not join the active crew");
        helper.assertTrue(DinosaurOwnership.setCommandMode(player, dinosaur, DinosaurCommandMode.FOLLOW).success(),
                "The quarry test companion could not enter Follow mode");
        dinosaur.setInvulnerable(true);
        dinosaur.assignFieldWork(new DinoWhistleSettings(
                DinoWhistleSettings.FieldMode.QUARRY,
                DinoWhistleSettings.Pattern.SINGLE,
                false,
                48
        ), target, null);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(targetRelative).isAir(),
                        "The follower never completed its server-authoritative quarry order; position="
                                + dinosaur.position() + ", mode=" + dinosaur.getCommandMode()
                                + ", fieldEnabled=" + dinosaur.hasFieldWork()
                                + ", hunger=" + dinosaur.getHunger()
                                + ", action=" + dinosaur.getWorkAction()
                                + ", entityTicks=" + dinosaur.tickCount))
                .thenExecute(() -> helper.assertTrue(!dinosaur.hasFieldWork(),
                        "A one-time quarry order remained active after its target was finished"))
                .thenSucceed();
    }

    private static void nightShiftDrainsMood(GameTestHelper helper) {
        BlockPos tableRelative = new BlockPos(0, 1, 0);
        BlockPos dodoRelative = new BlockPos(0, 2, 0);
        forceTicking(helper, tableRelative, dodoRelative);
        helper.setBlock(BlockPos.ZERO, Blocks.STONE);
        helper.setBlock(tableRelative, ModBlocks.COMMAND_TABLE.get());
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                13_000L
        );

        FieldDodoEntity dodo = helper.spawn(ModEntities.FIELD_DODO.get(), dodoRelative);
        dodo.setInvulnerable(true);
        dodo.assignWork(
                0,
                helper.absolutePos(tableRelative),
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                0,
                1,
                1,
                2,
                0,
                0,
                0,
                1,
                true,
                true
        );

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        dodo.tickCount >= 300,
                        "The night-shift dinosaur's chunk did not produce 300 entity ticks; entityTicks=" + dodo.tickCount
                                + ", alive=" + dodo.isAlive()
                                + ", removed=" + dodo.isRemoved()
                                + ", position=" + dodo.position()
                                + ", table=" + dodo.getCommandTablePos()
                ))
                .thenExecute(() -> helper.assertTrue(
                        dodo.getMood() == 67,
                        "Night duty should drain one mood point within 300 entity ticks; mood=" + dodo.getMood()
                                + ", alive=" + dodo.isAlive()
                                + ", enabled=" + dodo.isWorkEnabled()
                                + ", schedule=" + dodo.getWorkSchedule()
                                + ", table=" + dodo.getCommandTablePos()
                                + ", hunger=" + dodo.getHunger()
                                + ", sleeping=" + dodo.isDinosaurSleeping()
                                + ", target=" + dodo.getTarget()
                                + ", clock=" + dodo.level().getDefaultClockTime()
                                + ", drainUnits=" + dodo.getWorkMoodDrainUnits()
                                + ", entityTicks=" + dodo.tickCount
                ))
                .thenSucceed();
    }

    private static void dinosaurSleepsAtNight(GameTestHelper helper) {
        BlockPos dodoRelative = new BlockPos(0, 1, 0);
        forceTicking(helper, dodoRelative);
        helper.setBlock(BlockPos.ZERO, Blocks.STONE);
        helper.getLevel().clockManager().setTotalTicks(
                helper.getLevel().dimensionType().defaultClock().orElseThrow(),
                18_000L
        );
        FieldDodoEntity dodo = helper.spawn(ModEntities.FIELD_DODO.get(), dodoRelative);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        dodo.isDinosaurSleeping(),
                        "A safe, fed dinosaur did not settle to sleep at night"
                ))
                .thenExecuteAfter(80, () -> helper.assertTrue(
                        dodo.isDinosaurSleeping(),
                        "The dinosaur woke immediately instead of remaining asleep"
                ))
                .thenSucceed();
    }
}
