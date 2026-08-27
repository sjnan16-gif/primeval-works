package com.primevalworks.world.work;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DinoFieldWorkRulesTest {
    @Test
    void whistleRangeIsAlwaysKeptInsideTheServerLimit() {
        assertEquals(16, DinoWhistleRules.clampRange(-50));
        assertEquals(85, DinoWhistleRules.clampRange(500));
        assertEquals(40, DinoWhistleRules.clampRange(40));
    }

    @Test
    void areaOrdersRejectHugeVolumesAndLongThinSelections() {
        assertTrue(DinoFieldWorkLimits.areaWithinLimits(0, 0, 0, 7, 7, 7));
        assertFalse(DinoFieldWorkLimits.areaWithinLimits(0, 0, 0, 8, 8, 8));
        assertFalse(DinoFieldWorkLimits.areaWithinLimits(0, 0, 0, 16, 0, 0));
    }

    @Test
    void connectedAndAreaCapsStayBounded() {
        assertEquals(64, DinoFieldWorkLimits.MAX_CONNECTED_BLOCKS);
        assertEquals(512, DinoFieldWorkLimits.MAX_AREA_BLOCKS);
        assertEquals(16, DinoFieldWorkLimits.MAX_AREA_SPAN);
    }

    @Test
    void commandModesHaveClearPlayerFacingExplanations() {
        for (DinosaurCommandMode mode : DinosaurCommandMode.values()) {
            assertFalse(mode.title().isBlank());
            assertTrue(mode.description().length() >= 24);
        }
    }

    @Test
    void onlyTheFiveSpeciesSpecialistsReceiveOneFieldSpecialty() {
        assertEquals(DinoFieldSpecialtyProfile.Role.COLLECT,
                DinoFieldSpecialtyProfile.VELOCIRAPTOR.role());
        assertEquals(DinoFieldSpecialtyProfile.Role.QUARRY,
                DinoFieldSpecialtyProfile.TYRANNOSAURUS.role());
        assertEquals(DinoFieldSpecialtyProfile.Role.QUARRY,
                DinoFieldSpecialtyProfile.SPINOSAURUS.role());
        assertEquals(DinoFieldSpecialtyProfile.Role.LUMBER,
                DinoFieldSpecialtyProfile.PARASAUROLOPHUS.role());
        assertEquals(DinoFieldSpecialtyProfile.Role.HARVEST,
                DinoFieldSpecialtyProfile.DODO.role());

        assertEquals(5, List.of(
                        DinoFieldSpecialtyProfile.TYRANNOSAURUS,
                        DinoFieldSpecialtyProfile.TRICERATOPS,
                        DinoFieldSpecialtyProfile.VELOCIRAPTOR,
                        DinoFieldSpecialtyProfile.STEGOSAURUS,
                        DinoFieldSpecialtyProfile.PARASAUROLOPHUS,
                        DinoFieldSpecialtyProfile.PTERANODON,
                        DinoFieldSpecialtyProfile.DODO,
                        DinoFieldSpecialtyProfile.SPINOSAURUS
                ).stream().filter(DinoFieldSpecialtyProfile::eligible).count());
        assertFalse(DinoFieldSpecialtyProfile.TRICERATOPS.eligible());
        assertFalse(DinoFieldSpecialtyProfile.STEGOSAURUS.eligible());
        assertFalse(DinoFieldSpecialtyProfile.PTERANODON.eligible());
    }

    @Test
    void eachFieldSpecialtyUsesTheSpeciesBestBaseJob() {
        for (DinoFieldSpecialtyProfile fieldProfile : List.of(
                DinoFieldSpecialtyProfile.TYRANNOSAURUS,
                DinoFieldSpecialtyProfile.VELOCIRAPTOR,
                DinoFieldSpecialtyProfile.PARASAUROLOPHUS,
                DinoFieldSpecialtyProfile.DODO,
                DinoFieldSpecialtyProfile.SPINOSAURUS)) {
            int source = fieldProfile.sourceJobIndex();
            if (source < 0) continue;
            DinoSpeciesWorkProfile profile = DinoSpeciesWorkProfile.valueOf(fieldProfile.name());
            int best = 0;
            for (int job = 0; job < 5; job++) best = Math.max(best, profile.stars(job));
            assertEquals(best, profile.stars(source));
        }
    }

    @Test
    void everyWhistleModeExplainsItsOwnTargets() {
        assertEquals("Nearby blocks", DinoWhistleSettings.FieldMode.QUARRY.targetTitle(
                DinoWhistleSettings.Pattern.CONNECTED));
        assertEquals("Tree", DinoWhistleSettings.FieldMode.LUMBER.targetTitle(
                DinoWhistleSettings.Pattern.CONNECTED));
        assertEquals("Nearby crops", DinoWhistleSettings.FieldMode.HARVEST.targetTitle(
                DinoWhistleSettings.Pattern.AREA));
        assertEquals("Nearby items", DinoWhistleSettings.FieldMode.COLLECT.targetTitle(
                DinoWhistleSettings.Pattern.SINGLE));
        for (DinoWhistleSettings.FieldMode mode : DinoWhistleSettings.FieldMode.values()) {
            for (DinoWhistleSettings.Pattern pattern : DinoWhistleSettings.Pattern.values()) {
                assertFalse(mode.targetDescription(pattern).isBlank());
                assertFalse(mode.markHint(pattern).isBlank());
            }
        }
    }

    @Test
    void modesNormalizeToOnlyTheSupportedPlayerBehaviors() {
        assertEquals(DinoWhistleSettings.Pattern.AREA,
                DinoWhistleSettings.FieldMode.QUARRY.normalizePattern(DinoWhistleSettings.Pattern.SINGLE));
        assertEquals(DinoWhistleSettings.Pattern.AREA,
                DinoWhistleSettings.FieldMode.QUARRY.normalizePattern(DinoWhistleSettings.Pattern.AREA));
        assertEquals(DinoWhistleSettings.Pattern.CONNECTED,
                DinoWhistleSettings.FieldMode.LUMBER.normalizePattern(DinoWhistleSettings.Pattern.AREA));
        assertEquals(DinoWhistleSettings.Pattern.AREA,
                DinoWhistleSettings.FieldMode.HARVEST.normalizePattern(DinoWhistleSettings.Pattern.CONNECTED));
        assertEquals(DinoWhistleSettings.Pattern.AREA,
                DinoWhistleSettings.FieldMode.COLLECT.normalizePattern(DinoWhistleSettings.Pattern.CONNECTED));
        assertTrue(DinoWhistleSettings.FieldMode.QUARRY.isPassive());
        assertTrue(DinoWhistleSettings.FieldMode.LUMBER.requiresMark());
        assertTrue(DinoWhistleSettings.FieldMode.HARVEST.isPassive());
        assertTrue(DinoWhistleSettings.FieldMode.COLLECT.isPassive());
    }
}
