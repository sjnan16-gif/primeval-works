package com.primevalworks.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class PrimevalConfig {
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();

        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        SERVER = new Server(serverBuilder);
        SERVER_SPEC = serverBuilder.build();
    }

    private PrimevalConfig() {
    }

    public static void loadServer(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SERVER_SPEC) return;
        PrimevalTuning.apply(new PrimevalTuning.Server(
                SERVER.startingFollowerSlots.get(), SERVER.followerSlotsPerFieldCommandRank.get(),
                SERVER.maximumFollowerSlots.get(),
                SERVER.hungerDrainRate.get(), SERVER.moodDrainRate.get(), SERVER.nightShiftMoodRate.get(),
                SERVER.foodBoxThreshold.get(), SERVER.recoveryTime.get(), SERVER.breedingCooldown.get(),
                SERVER.wildHugeChance.get(), SERVER.wildAlbinoChance.get(),
                SERVER.incubatedHugeChance.get(), SERVER.incubatedAlbinoChance.get(),
                SERVER.bredHugeChance.get(), SERVER.bredAlbinoChance.get(),
                SERVER.parentMutationInheritanceChance.get(), SERVER.parentMutationLevelBonus.get(),
                SERVER.parentMutationQualityBonus.get(),
                SERVER.hugeScale.get(), SERVER.hugeStats.get(), SERVER.albinoStats.get(), SERVER.albinoHealth.get(),
                SERVER.workSpeed.get(), SERVER.transportCapacity.get(), SERVER.energyGeneration.get(),
                SERVER.targetsPerWorkOrder.get(), SERVER.energyStorage.get(), SERVER.waterTurbineOutput.get(),
                SERVER.expeditionTime.get(), SERVER.expeditionRisk.get(), SERVER.expeditionRewards.get(),
                SERVER.pteranodonFlightSpeed.get(), SERVER.pteranodonStaminaDrain.get(),
                SERVER.pteranodonStaminaRecovery.get(), SERVER.spinosaurusSwimSpeed.get(),
                SERVER.spinosaurusSprintSpeed.get(), SERVER.spinosaurusStaminaDrain.get(),
                SERVER.spinosaurusStaminaRecovery.get(), SERVER.dinosaurHealth.get(), SERVER.dinosaurDamage.get(),
                SERVER.hostileMobTargeting.get(), SERVER.turretDamage.get(), SERVER.turretRange.get(),
                SERVER.machineEnergyUse.get(), SERVER.processorSpeed.get(), SERVER.ancientFurnaceSpeed.get(),
                SERVER.incubatorSpeed.get()
        ));
    }

    public static void unloadServer(ModConfigEvent.Unloading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) PrimevalTuning.reset();
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue uiSounds;
        public final ModConfigSpec.DoubleValue uiSoundVolume;
        public final ModConfigSpec.BooleanValue heavyFootsteps;
        public final ModConfigSpec.DoubleValue heavyFootstepVolume;
        public final ModConfigSpec.DoubleValue heavyFootstepRange;
        public final ModConfigSpec.DoubleValue footstepShakeStrength;
        public final ModConfigSpec.BooleanValue footstepDust;
        public final ModConfigSpec.BooleanValue pteranodonWind;
        public final ModConfigSpec.DoubleValue pteranodonWindVolume;
        public final ModConfigSpec.DoubleValue mountFovStrength;
        public final ModConfigSpec.DoubleValue mountBankStrength;
        public final ModConfigSpec.BooleanValue staminaHud;
        public final ModConfigSpec.BooleanValue hatchReveal;
        public final ModConfigSpec.DoubleValue unmountedSpinosaurusGaitSpeed;

        private Client(ModConfigSpec.Builder builder) {
            builder.comment("Sound, animation and camera feedback that only changes what you see and hear.")
                    .translation("config.primevalworks.client.feedback")
                    .push("feedback");
            uiSounds = bool(builder, "uiSounds", true,
                    "Plays the small open, close, button and upgrade sounds in Primeval Works menus.");
            uiSoundVolume = decimal(builder, "uiSoundVolume", 0.75D, 0.0D, 1.5D,
                    "Turns every Primeval Works menu sound up or down without touching Minecraft's master volume.");
            heavyFootsteps = bool(builder, "heavyFootsteps", true,
                    "Plays close-range foot contacts for the large dinosaurs. The timing comes from their animation keyframes.");
            heavyFootstepVolume = decimal(builder, "heavyFootstepVolume", 1.0D, 0.0D, 2.0D,
                    "Controls how strong a heavy dinosaur sounds while you are standing right beside it.");
            heavyFootstepRange = decimal(builder, "heavyFootstepRange", 5.5D, 1.5D, 12.0D,
                    "The hard hearing limit for heavy footsteps. They fade sharply before reaching this distance.");
            footstepShakeStrength = decimal(builder, "footstepShakeStrength", 1.0D, 0.0D, 2.0D,
                    "Scales the brief camera impulse from a running Tyrannosaurus or mounted Spinosaurus. Set this to zero to remove it.");
            footstepDust = bool(builder, "footstepDust", true,
                    "Kicks up a few particles at an authored heavy-foot contact.");
            pteranodonWind = bool(builder, "pteranodonWind", true,
                    "Plays a private Elytra-like airflow loop while you fly a Pteranodon.");
            pteranodonWindVolume = decimal(builder, "pteranodonWindVolume", 0.85D, 0.0D, 2.0D,
                    "Controls the Pteranodon airflow. Speed still changes its volume and pitch in real time.");
            mountFovStrength = decimal(builder, "mountFovStrength", 1.0D, 0.0D, 2.0D,
                    "Scales speed-based field-of-view changes while flying or sprinting a mount.");
            mountBankStrength = decimal(builder, "mountBankStrength", 1.0D, 0.0D, 2.0D,
                    "Scales the smooth camera lean while a flying or swimming mount turns.");
            staminaHud = bool(builder, "staminaHud", true,
                    "Shows the compact stamina bar while a Pteranodon flies or a Spinosaurus sprints on land.");
            hatchReveal = bool(builder, "hatchReveal", true,
                    "Shows the top-right hatch card with the new dinosaur's name, quality and mutations.");
            unmountedSpinosaurusGaitSpeed = decimal(builder, "unmountedSpinosaurusGaitSpeed", 0.70D, 0.35D, 1.25D,
                    "Scales only the unmounted land-walk animation. Riding, swimming and sprinting keep their own timing.");
            builder.pop();
        }
    }

    public static final class Server {
        public final ModConfigSpec.IntValue startingFollowerSlots;
        public final ModConfigSpec.IntValue followerSlotsPerFieldCommandRank;
        public final ModConfigSpec.IntValue maximumFollowerSlots;

        public final ModConfigSpec.DoubleValue hungerDrainRate;
        public final ModConfigSpec.DoubleValue moodDrainRate;
        public final ModConfigSpec.DoubleValue nightShiftMoodRate;
        public final ModConfigSpec.IntValue foodBoxThreshold;
        public final ModConfigSpec.DoubleValue recoveryTime;
        public final ModConfigSpec.DoubleValue breedingCooldown;

        public final ModConfigSpec.DoubleValue wildHugeChance;
        public final ModConfigSpec.DoubleValue wildAlbinoChance;
        public final ModConfigSpec.DoubleValue incubatedHugeChance;
        public final ModConfigSpec.DoubleValue incubatedAlbinoChance;
        public final ModConfigSpec.DoubleValue bredHugeChance;
        public final ModConfigSpec.DoubleValue bredAlbinoChance;
        public final ModConfigSpec.DoubleValue parentMutationInheritanceChance;
        public final ModConfigSpec.DoubleValue parentMutationLevelBonus;
        public final ModConfigSpec.DoubleValue parentMutationQualityBonus;
        public final ModConfigSpec.DoubleValue hugeScale;
        public final ModConfigSpec.DoubleValue hugeStats;
        public final ModConfigSpec.DoubleValue albinoStats;
        public final ModConfigSpec.DoubleValue albinoHealth;

        public final ModConfigSpec.DoubleValue workSpeed;
        public final ModConfigSpec.DoubleValue transportCapacity;
        public final ModConfigSpec.DoubleValue energyGeneration;
        public final ModConfigSpec.IntValue targetsPerWorkOrder;
        public final ModConfigSpec.DoubleValue energyStorage;
        public final ModConfigSpec.DoubleValue waterTurbineOutput;

        public final ModConfigSpec.DoubleValue expeditionTime;
        public final ModConfigSpec.DoubleValue expeditionRisk;
        public final ModConfigSpec.DoubleValue expeditionRewards;

        public final ModConfigSpec.DoubleValue pteranodonFlightSpeed;
        public final ModConfigSpec.DoubleValue pteranodonStaminaDrain;
        public final ModConfigSpec.DoubleValue pteranodonStaminaRecovery;
        public final ModConfigSpec.DoubleValue spinosaurusSwimSpeed;
        public final ModConfigSpec.DoubleValue spinosaurusSprintSpeed;
        public final ModConfigSpec.DoubleValue spinosaurusStaminaDrain;
        public final ModConfigSpec.DoubleValue spinosaurusStaminaRecovery;

        public final ModConfigSpec.DoubleValue dinosaurHealth;
        public final ModConfigSpec.DoubleValue dinosaurDamage;
        public final ModConfigSpec.BooleanValue hostileMobTargeting;
        public final ModConfigSpec.DoubleValue turretDamage;
        public final ModConfigSpec.DoubleValue turretRange;

        public final ModConfigSpec.DoubleValue machineEnergyUse;
        public final ModConfigSpec.DoubleValue processorSpeed;
        public final ModConfigSpec.DoubleValue ancientFurnaceSpeed;
        public final ModConfigSpec.DoubleValue incubatorSpeed;

        private Server(ModConfigSpec.Builder builder) {
            builder.comment("Travel-crew capacity. Every limit is enforced by the server and supports up to four followers.")
                    .translation("config.primevalworks.server.followers")
                    .push("followers");
            startingFollowerSlots = integer(builder, "startingFollowerSlots", 1, 1, 4,
                    "Follower slots available at a fresh Command Table.");
            followerSlotsPerFieldCommandRank = integer(builder, "followerSlotsPerFieldCommandRank", 1, 0, 3,
                    "Follower slots added by each of the two Field Command ranks.");
            maximumFollowerSlots = integer(builder, "maximumFollowerSlots", 3, 1, 4,
                    "Final follower cap. If this is below the starting value, the starting value wins.");
            builder.pop();

            builder.comment("Everyday companion needs and recovery timing.")
                    .translation("config.primevalworks.server.needs")
                    .push("needs");
            hungerDrainRate = decimal(builder, "hungerDrainRate", 1.0D, 0.0D, 5.0D,
                    "How quickly hunger falls. Zero turns passive and work hunger loss off.");
            moodDrainRate = decimal(builder, "moodDrainRate", 1.0D, 0.0D, 5.0D,
                    "How quickly ordinary work wears down mood. Zero disables work mood loss.");
            nightShiftMoodRate = decimal(builder, "nightShiftMoodRate", 2.3D, 1.0D, 8.0D,
                    "The total mood-drain multiplier for night-shift work. The normal balance is 2.3x.");
            foodBoxThreshold = integer(builder, "foodBoxThreshold", 50, 1, 99,
                    "A dinosaur heads to a Food Box when hunger reaches this percentage or lower.");
            recoveryTime = decimal(builder, "recoveryTime", 1.0D, 0.05D, 10.0D,
                    "Scales the time an injured dinosaur spends in the recovery row.");
            breedingCooldown = decimal(builder, "breedingCooldown", 1.0D, 0.0D, 10.0D,
                    "Scales the wait before either parent can breed again. Zero removes the cooldown.");
            builder.pop();

            builder.comment("Mutation odds and the bonuses attached to them. Chances use 0.01 for one percent.")
                    .translation("config.primevalworks.server.genetics")
                    .push("genetics");
            wildHugeChance = decimal(builder, "wildHugeChance", 0.05D, 0.0D, 1.0D,
                    "Chance for a normally hatched dinosaur to be Huge.");
            wildAlbinoChance = decimal(builder, "wildAlbinoChance", 0.005D, 0.0D, 1.0D,
                    "Chance for a normally hatched dinosaur to be Albino.");
            incubatedHugeChance = decimal(builder, "incubatedHugeChance", 0.06D, 0.0D, 1.0D,
                    "Huge chance for an egg completed in the Premium Egg Incubator.");
            incubatedAlbinoChance = decimal(builder, "incubatedAlbinoChance", 0.01D, 0.0D, 1.0D,
                    "Albino chance for an egg completed in the Premium Egg Incubator.");
            bredHugeChance = decimal(builder, "bredHugeChance", 0.09D, 0.0D, 1.0D,
                    "Huge chance for a bred egg when neither parent is Huge.");
            bredAlbinoChance = decimal(builder, "bredAlbinoChance", 0.012D, 0.0D, 1.0D,
                    "Albino chance for a bred egg when neither parent is Albino.");
            parentMutationInheritanceChance = decimal(builder, "parentMutationInheritanceChance", 0.09D, 0.0D, 1.0D,
                    "Base chance for each mutation-carrying parent to pass that mutation to a bred egg.");
            parentMutationLevelBonus = decimal(builder, "parentMutationLevelBonus", 0.06D, 0.0D, 1.0D,
                    "Extra inheritance chance a level 100 parent contributes. Lower levels scale smoothly.");
            parentMutationQualityBonus = decimal(builder, "parentMutationQualityBonus", 0.06D, 0.0D, 1.0D,
                    "Extra inheritance chance a 100-quality parent contributes. Lower quality scales smoothly.");
            hugeScale = decimal(builder, "hugeScale", 1.18D, 1.0D, 2.0D,
                    "Visual and collision scale for Huge dinosaurs.");
            hugeStats = decimal(builder, "hugeStats", 1.20D, 0.25D, 5.0D,
                    "Work, movement, health and damage multiplier from Huge.");
            albinoStats = decimal(builder, "albinoStats", 1.40D, 0.25D, 5.0D,
                    "Work, movement, damage and mount-speed multiplier from Albino.");
            albinoHealth = decimal(builder, "albinoHealth", 0.80D, 0.10D, 3.0D,
                    "Albino maximum-health multiplier. Values below one keep the intended glass-cannon tradeoff.");
            builder.pop();

            builder.comment("Base automation speed, routing limits and power output.")
                    .translation("config.primevalworks.server.automation")
                    .push("automation");
            workSpeed = decimal(builder, "workSpeed", 1.0D, 0.10D, 10.0D,
                    "Scales every dinosaur work action. Higher values finish actions faster.");
            transportCapacity = decimal(builder, "transportCapacity", 1.0D, 0.10D, 10.0D,
                    "Scales how many items transport dinosaurs can carry per trip.");
            energyGeneration = decimal(builder, "energyGeneration", 1.0D, 0.0D, 10.0D,
                    "Scales energy produced by assigned dinosaurs.");
            targetsPerWorkOrder = integer(builder, "targetsPerWorkOrder", 8, 1, 8,
                    "Maximum selected sources, workstations or destinations in one order.");
            energyStorage = decimal(builder, "energyStorage", 1.0D, 0.10D, 20.0D,
                    "Scales the Command Table's stored-energy capacity.");
            waterTurbineOutput = decimal(builder, "waterTurbineOutput", 1.5D, 0.0D, 10.0D,
                    "Output multiplier for a correctly submerged Water Turbine. Wind Turbines stay at 1x.");
            builder.pop();

            builder.comment("Expedition clocks, injury rolls and reward stack sizes.")
                    .translation("config.primevalworks.server.expeditions")
                    .push("expeditions");
            expeditionTime = decimal(builder, "expeditionTime", 1.0D, 0.05D, 10.0D,
                    "Scales expedition duration. Lower values bring dinosaurs home sooner.");
            expeditionRisk = decimal(builder, "expeditionRisk", 1.0D, 0.0D, 5.0D,
                    "Scales incapacitation chance after specialty, level and mutation bonuses.");
            expeditionRewards = decimal(builder, "expeditionRewards", 1.0D, 0.0D, 10.0D,
                    "Scales the number of items returned by expeditions.");
            builder.pop();

            builder.comment("Mounted travel speed and stamina economy.")
                    .translation("config.primevalworks.server.mounts")
                    .push("mounts");
            pteranodonFlightSpeed = decimal(builder, "pteranodonFlightSpeed", 1.0D, 0.20D, 4.0D,
                    "Scales Pteranodon cruise, boost and glide speed together.");
            pteranodonStaminaDrain = decimal(builder, "pteranodonStaminaDrain", 1.0D, 0.0D, 5.0D,
                    "Scales stamina spent by powered Pteranodon flight and climbing.");
            pteranodonStaminaRecovery = decimal(builder, "pteranodonStaminaRecovery", 1.0D, 0.0D, 5.0D,
                    "Scales Pteranodon recovery on the ground and while gliding.");
            spinosaurusSwimSpeed = decimal(builder, "spinosaurusSwimSpeed", 1.0D, 0.20D, 4.0D,
                    "Scales mounted Spinosaurus swimming and breach momentum.");
            spinosaurusSprintSpeed = decimal(builder, "spinosaurusSprintSpeed", 1.0D, 0.20D, 4.0D,
                    "Scales mounted Spinosaurus land sprint speed without changing normal walking.");
            spinosaurusStaminaDrain = decimal(builder, "spinosaurusStaminaDrain", 1.0D, 0.0D, 5.0D,
                    "Scales stamina spent by a Spinosaurus land sprint.");
            spinosaurusStaminaRecovery = decimal(builder, "spinosaurusStaminaRecovery", 1.0D, 0.0D, 5.0D,
                    "Scales Spinosaurus land-stamina recovery.");
            builder.pop();

            builder.comment("Companion durability and base-defense strength.")
                    .translation("config.primevalworks.server.combat")
                    .push("combat");
            dinosaurHealth = decimal(builder, "dinosaurHealth", 1.0D, 0.10D, 10.0D,
                    "Scales maximum health for every dinosaur after quality, level and mutations.");
            dinosaurDamage = decimal(builder, "dinosaurDamage", 1.0D, 0.0D, 10.0D,
                    "Scales melee damage for combat-capable dinosaurs.");
            hostileMobTargeting = bool(builder, "hostileMobTargeting", true,
                    "Lets idle Tyrannosaurus and Spinosaurus proactively defend the base. Jobs always take priority, and hostile mobs do not target dinosaurs by default.");
            turretDamage = decimal(builder, "turretDamage", 1.0D, 0.0D, 10.0D,
                    "Scales Laser Turret damage.");
            turretRange = decimal(builder, "turretRange", 1.0D, 0.25D, 4.0D,
                    "Scales target range for the defensive turret.");
            builder.pop();

            builder.comment("Energy cost and processing speed for powered machines.")
                    .translation("config.primevalworks.server.machines")
                    .push("machines");
            machineEnergyUse = decimal(builder, "machineEnergyUse", 1.0D, 0.0D, 10.0D,
                    "Scales energy used by every connected machine. Zero makes connected machines free to run.");
            processorSpeed = decimal(builder, "processorSpeed", 1.0D, 0.10D, 10.0D,
                    "Scales Processor progress. Higher values finish recipes faster.");
            ancientFurnaceSpeed = decimal(builder, "ancientFurnaceSpeed", 1.0D, 0.10D, 10.0D,
                    "Scales Ancient Furnace progress after its own energy-speed lever.");
            incubatorSpeed = decimal(builder, "incubatorSpeed", 1.0D, 0.10D, 10.0D,
                    "Scales Premium Egg Incubator progress.");
            builder.pop();
        }
    }

    private static ModConfigSpec.BooleanValue bool(
            ModConfigSpec.Builder builder, String key, boolean defaultValue, String comment
    ) {
        return builder.comment(comment).translation(translation(key)).define(key, defaultValue);
    }

    private static ModConfigSpec.IntValue integer(
            ModConfigSpec.Builder builder, String key, int defaultValue, int minimum, int maximum, String comment
    ) {
        return builder.comment(comment).translation(translation(key))
                .defineInRange(key, defaultValue, minimum, maximum);
    }

    private static ModConfigSpec.DoubleValue decimal(
            ModConfigSpec.Builder builder, String key, double defaultValue, double minimum, double maximum,
            String comment
    ) {
        return builder.comment(comment).translation(translation(key))
                .defineInRange(key, defaultValue, minimum, maximum);
    }

    private static String translation(String key) {
        return "config.primevalworks.option." + key;
    }
}
