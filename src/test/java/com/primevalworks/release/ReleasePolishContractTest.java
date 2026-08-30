package com.primevalworks.release;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleasePolishContractTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path SOURCE = Path.of("src/main/java/com/primevalworks");

    @Test
    void ordinaryMachinesHaveMiningToolsAndOnlyCommandTableNeedsDiamond() throws Exception {
        String pickaxe = Files.readString(RESOURCES.resolve("data/minecraft/tags/block/mineable/pickaxe.json"));
        String axe = Files.readString(RESOURCES.resolve("data/minecraft/tags/block/mineable/axe.json"));
        String diamond = Files.readString(RESOURCES.resolve("data/minecraft/tags/block/needs_diamond_tool.json"));

        for (String block : new String[]{"wind_turbine", "water_turbine", "processor", "ancient_furnace",
                "laser_observer", "laser_turret", "premium_egg_incubator"}) {
            assertTrue(pickaxe.contains("primevalworks:" + block), block);
        }
        assertTrue(axe.contains("primevalworks:food_box"));
        assertTrue(axe.contains("primevalworks:ancient_barrel"));
        assertTrue(diamond.contains("primevalworks:command_table"));
        assertTrue(diamond.contains("primevalworks:command_table_extension"));
        assertFalse(diamond.contains("primevalworks:processor"));
        assertFalse(diamond.contains("primevalworks:water_turbine"));
    }

    @Test
    void invisibleWaterTurbinePartsUseARealParticleTexture() throws Exception {
        String state = Files.readString(RESOURCES.resolve("assets/primevalworks/blockstates/turbine_part.json"));
        String model = Files.readString(RESOURCES.resolve("assets/primevalworks/models/block/turbine_part.json"));

        assertFalse(state.contains("minecraft:block/air"));
        assertTrue(state.contains("primevalworks:block/turbine_part"));
        assertTrue(model.contains("\"particle\""));
        assertTrue(model.contains("minecraft:block/dark_oak_planks"));
    }

    @Test
    void plannerRefreshesHighlightsImmediately() throws Exception {
        String planner = Files.readString(SOURCE.resolve("client/screen/WorksitePlannerScreen.java"));

        assertTrue(planner.contains("scanWorkstationIndexStep(500_000)"));
        assertFalse(planner.contains("scanWorkstationIndexStep(6_000)"));
    }

    @Test
    void hostilesCannotAcquireDinosaursAsTargets() throws Exception {
        String entrypoint = Files.readString(SOURCE.resolve("PrimevalWorks.java"));
        String protection = Files.readString(SOURCE.resolve("world/entity/DinosaurTargetProtection.java"));
        String dinosaur = Files.readString(SOURCE.resolve("world/entity/FieldDodoEntity.java"));

        assertTrue(entrypoint.contains("DinosaurTargetProtection::preventHostileTargeting"));
        assertTrue(protection.contains("event.getEntity() instanceof Enemy"));
        assertTrue(protection.contains("event.getNewAboutToBeSetTarget() instanceof FieldDodoEntity"));
        assertTrue(protection.contains("event.setNewAboutToBeSetTarget(null)"));
        assertFalse(dinosaur.contains("threat.setTarget(this)"));
    }

    @Test
    void uiLessPoweredBlocksReportTheirEnergyState() throws Exception {
        String observer = Files.readString(SOURCE.resolve("world/block/PoweredObserverBlock.java"));
        String turret = Files.readString(SOURCE.resolve("world/block/LaserTurretBlock.java"));
        String energy = Files.readString(SOURCE.resolve("world/base/BaseEnergyRules.java"));

        assertTrue(observer.contains("showEnergyStatus"));
        assertTrue(turret.contains("showEnergyStatus"));
        assertTrue(energy.contains("This block requires energy"));
    }

    @Test
    void releaseCommandsKeepUsefulToolsWithoutObsoleteInsightOrDebugEggGroups() throws Exception {
        String commands = Files.readString(SOURCE.resolve("command/PrimevalCommands.java"));
        String items = Files.readString(SOURCE.resolve("registry/ModItems.java"));
        String tab = Files.readString(SOURCE.resolve("registry/ModCreativeTabs.java"));

        for (String command : new String[]{"help", "roster", "recall", "egg", "mutation", "hatch"}) {
            assertTrue(commands.contains("Commands.literal(\"" + command + "\")"), command);
        }
        assertFalse(commands.contains("Commands.literal(\"insight\")"));
        assertFalse(items.contains("DEBUG_SPAWN_EGGS"));
        assertFalse(tab.contains("isDebugSpawnEgg"));
    }
}
