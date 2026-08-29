package com.primevalworks.world.work;

import org.junit.jupiter.api.Test;
import com.primevalworks.world.entity.DinosaurSpecies;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorkSpecialtyRulesTest {
    @Test
    void specialtyItemCapacitiesDoNotLeakTransportRules() {
        assertArrayEquals(new int[]{4, 3, 0, 1, 0}, new int[]{
                WorkSpecialtyRules.itemFilterCapacity(0),
                WorkSpecialtyRules.itemFilterCapacity(1),
                WorkSpecialtyRules.itemFilterCapacity(2),
                WorkSpecialtyRules.itemFilterCapacity(3),
                WorkSpecialtyRules.itemFilterCapacity(4)
        });
        assertEquals(4, WorkSpecialtyRules.fuelFilterCapacity(1));
        assertEquals(0, WorkSpecialtyRules.fuelFilterCapacity(0));
        assertEquals(0, WorkSpecialtyRules.fuelFilterCapacity(2));
    }

    @Test
    void fourStarExpeditionsScaleFromTenToThirtyFourMinutes() {
        assertArrayEquals(new int[]{10, 14, 19, 26, 34}, new int[]{
                WorkSpecialtyRules.expeditionDurationMinutes(0),
                WorkSpecialtyRules.expeditionDurationMinutes(1),
                WorkSpecialtyRules.expeditionDurationMinutes(2),
                WorkSpecialtyRules.expeditionDurationMinutes(3),
                WorkSpecialtyRules.expeditionDurationMinutes(4)
        });
        assertEquals(12_000L, WorkSpecialtyRules.expeditionDurationTicks(0));
        assertEquals(40_800L, WorkSpecialtyRules.expeditionDurationTicks(4));
    }

    @Test
    void expeditionRiskAndRewardIncreaseMonotonically() {
        int previousRisk = -1;
        int previousReward = 0;
        for (int tier = 0; tier < 5; tier++) {
            int risk = WorkSpecialtyRules.expeditionRiskPercent(tier);
            int reward = WorkSpecialtyRules.expeditionRewardCount(tier);
            org.junit.jupiter.api.Assertions.assertTrue(risk > previousRisk);
            org.junit.jupiter.api.Assertions.assertTrue(reward > previousReward);
            previousRisk = risk;
            previousReward = reward;
        }
    }

    @Test
    void weakSpecialtiesRunAtTwentyPercentOfBaseline() {
        assertArrayEquals(new int[]{20, 20, 45, 65, 100}, new int[]{
                WorkSpecialtyRules.efficiencyPercent(0),
                WorkSpecialtyRules.efficiencyPercent(1),
                WorkSpecialtyRules.efficiencyPercent(2),
                WorkSpecialtyRules.efficiencyPercent(3),
                WorkSpecialtyRules.efficiencyPercent(4)
        });
        assertEquals(500, WorkSpecialtyRules.actionDurationTicks(100, 1));
        assertEquals(100, WorkSpecialtyRules.actionDurationTicks(100, 4));
    }

    @Test
    void energyOutputScalesFromOnePointFiveToElevenPerSecond() {
        assertEquals(1.275F, WorkSpecialtyRules.energyPerSecond(0), 0.0001F);
        assertEquals(1.7F, WorkSpecialtyRules.energyPerSecond(1), 0.0001F);
        assertEquals(3.825F, WorkSpecialtyRules.energyPerSecond(2), 0.0001F);
        assertEquals(6.375F, WorkSpecialtyRules.energyPerSecond(3), 0.0001F);
        assertEquals(9.35F, WorkSpecialtyRules.energyPerSecond(4), 0.0001F);
        assertTrue(WorkSpecialtyRules.energyPerSecond(4, 100) > WorkSpecialtyRules.energyPerSecond(4, 1));
    }

    @Test
    void transportCapacityScalesWithAptitudeAndLevel() {
        assertEquals(9, WorkSpecialtyRules.transportCapacity(1, 1, 64, 64));
        assertEquals(41, WorkSpecialtyRules.transportCapacity(3, 1, 64, 64));
        assertEquals(64, WorkSpecialtyRules.transportCapacity(4, 100, 64, 64));
    }

    @Test
    void finalExpeditionUsesTheAuthoredStarRiskAndDurationCurve() {
        assertTrue(!WorkSpecialtyRules.canAttemptExpedition(4, 1));
        assertTrue(WorkSpecialtyRules.canAttemptExpedition(4, 2));
        assertTrue(WorkSpecialtyRules.canAttemptExpedition(4, 3));
        assertTrue(WorkSpecialtyRules.canAttemptExpedition(4, 4));
        assertEquals(92, WorkSpecialtyRules.expeditionRiskPercent(4, 2, 1));
        assertEquals(64, WorkSpecialtyRules.expeditionRiskPercent(4, 3, 1));
        assertEquals(12, WorkSpecialtyRules.expeditionRiskPercent(4, 4, 1));
        assertEquals(120, WorkSpecialtyRules.expeditionDurationMinutes(4, 2));
        assertEquals(64, WorkSpecialtyRules.expeditionDurationMinutes(4, 3));
        assertEquals(34, WorkSpecialtyRules.expeditionDurationMinutes(4, 4));
        assertTrue(WorkSpecialtyRules.expeditionDurationMinutes(4, 4, 1.68F) >= 18);
        assertTrue(WorkSpecialtyRules.expeditionDurationMinutes(4, 4, 1.68F) <= 22);
        assertTrue(WorkSpecialtyRules.expeditionRiskPercent(4, 4, 1, 1.68F) < 12);
    }

    @Test
    void heavyCargoSlowsWeakTransportersMore() {
        assertEquals(1.0D, WorkSpecialtyRules.transportMovementMultiplier(1, 0, 64));
        double weakFullLoad = WorkSpecialtyRules.transportMovementMultiplier(1, 64, 64);
        double expertFullLoad = WorkSpecialtyRules.transportMovementMultiplier(4, 64, 64);
        assertTrue(weakFullLoad < expertFullLoad);
        assertEquals(0.70D, weakFullLoad, 0.0001D);
        assertEquals(0.92D, expertFullLoad, 0.0001D);
        assertEquals(28, WorkSpecialtyRules.transportHandlingDurationTicks(
                WorkSpecialtyRules.CHEST_EXTRACT_TICKS, 4, 1, 64));
        assertEquals(28, WorkSpecialtyRules.transportHandlingDurationTicks(
                WorkSpecialtyRules.CHEST_EXTRACT_TICKS, 4, 64, 64));
    }

    @Test
    void nightShiftDrainsOneHundredThirtyPercentMoreMood() {
        int regularDrain = WorkSpecialtyRules.workMoodDrainUnitsPerTick(0);
        int nightDrain = WorkSpecialtyRules.workMoodDrainUnitsPerTick(2);

        assertEquals(20, regularDrain);
        assertEquals(46, nightDrain);
        assertEquals(2.3D, nightDrain / (double) regularDrain, 0.0001D);
        assertEquals(2, regularDrain * 1_200 / WorkSpecialtyRules.WORK_MOOD_DRAIN_UNITS_PER_POINT);
        assertEquals(4, nightDrain * 1_200 / WorkSpecialtyRules.WORK_MOOD_DRAIN_UNITS_PER_POINT);
    }

    @Test
    void speciesProfilesKeepOnlyStrongJobsAboveOffSpecialtyCap() {
        assertArrayEquals(new int[]{2, 2, 1, 2, 4}, DinoSpeciesWorkProfile.DODO.specialtyStars());
        for (DinoSpeciesWorkProfile profile : DinoSpeciesWorkProfile.values()) {
            long strongJobs = Arrays.stream(profile.specialtyStars()).filter(stars -> stars >= 3).count();
            assertTrue(strongJobs >= 1 && strongJobs <= 2, profile.name());
        }
        assertArrayEquals(new int[]{1, 4, 1, 1, 4}, DinoSpeciesWorkProfile.TYRANNOSAURUS.specialtyStars());
        assertArrayEquals(new int[]{1, 2, 4, 1, 4}, DinoSpeciesWorkProfile.SPINOSAURUS.specialtyStars());
        assertArrayEquals(new int[]{4, 1, 1, 2, 2}, DinoSpeciesWorkProfile.VELOCIRAPTOR.specialtyStars());
        assertArrayEquals(new int[]{1, 1, 3, 3, 1}, DinoSpeciesWorkProfile.PARASAUROLOPHUS.specialtyStars());
    }

    @Test
    void activeSpeciesPassivesAreUniqueAndSpecialtyAligned() {
        DinosaurSpecies[] active = {
                DinosaurSpecies.TYRANNOSAURUS, DinosaurSpecies.TRICERATOPS,
                DinosaurSpecies.VELOCIRAPTOR, DinosaurSpecies.STEGOSAURUS,
                DinosaurSpecies.PARASAUROLOPHUS, DinosaurSpecies.PTERANODON,
                DinosaurSpecies.DODO, DinosaurSpecies.SPINOSAURUS
        };
        assertEquals(active.length,
                Arrays.stream(active).map(DinosaurSpecies::passiveTitle).distinct().count());
        assertTrue(DinosaurSpecies.TYRANNOSAURUS.passiveWorkSpeedMultiplier(1) > 1.0F);
        assertEquals(1.0F, DinosaurSpecies.VELOCIRAPTOR.passiveWorkSpeedMultiplier(0));
        assertTrue(DinosaurSpecies.SPINOSAURUS.passiveWorkSpeedMultiplier(2) > 1.0F);
        assertEquals(1.0F, DinosaurSpecies.SPINOSAURUS.passiveWorkSpeedMultiplier(0));
    }
}
