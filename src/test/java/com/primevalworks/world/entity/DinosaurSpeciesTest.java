package com.primevalworks.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DinosaurSpeciesTest {
    @Test
    void authoredModelsKeepStableGameplayHitboxes() {
        assertEquals(0.88F, DinosaurSpecies.DODO.collisionWidth());
        assertEquals(1.56F, DinosaurSpecies.DODO.collisionHeight());
        assertEquals(2.03F, DinosaurSpecies.TYRANNOSAURUS.collisionWidth());
        assertEquals(3.25F, DinosaurSpecies.TYRANNOSAURUS.collisionHeight());
        assertEquals(1.875F, DinosaurSpecies.TRICERATOPS.collisionWidth());
        assertEquals(3.375F, DinosaurSpecies.TRICERATOPS.collisionHeight());
        assertEquals(1.75F, DinosaurSpecies.STEGOSAURUS.collisionWidth());
        assertEquals(2.88F, DinosaurSpecies.STEGOSAURUS.collisionHeight());
        assertEquals(1.35F, DinosaurSpecies.PTERANODON.collisionWidth());
        assertEquals(1.25F, DinosaurSpecies.PTERANODON.collisionHeight());
        assertEquals(1.16F, DinosaurSpecies.PARASAUROLOPHUS.collisionWidth());
        assertEquals(3.31F, DinosaurSpecies.PARASAUROLOPHUS.collisionHeight());
        assertEquals(2.03F, DinosaurSpecies.SPINOSAURUS.collisionWidth());
        assertEquals(5.05F, DinosaurSpecies.SPINOSAURUS.collisionHeight());
        assertEquals(0.77F, DinosaurSpecies.VELOCIRAPTOR.collisionWidth());
        assertEquals(1.55F, DinosaurSpecies.VELOCIRAPTOR.collisionHeight());
    }

    @Test
    void everySpeciesHasAUsableCollisionContract() {
        for (DinosaurSpecies species : DinosaurSpecies.values()) {
            assertTrue(species.collisionWidth() >= 0.60F && species.collisionWidth() <= 2.10F,
                    species.registryName() + " has an unsafe collision width");
            assertTrue(species.collisionHeight() >= 0.80F && species.collisionHeight() <= 5.10F,
                    species.registryName() + " has an unsafe collision height");
        }
    }

    @Test
    void bodySizeChangesCareAndMovementWithoutChangingRenderScale() {
        assertTrue(DinosaurSpecies.TYRANNOSAURUS.appetite() > DinosaurSpecies.DODO.appetite());
        assertTrue(DinosaurSpecies.TYRANNOSAURUS.hungerDrainIntervalTicks()
                < DinosaurSpecies.DODO.hungerDrainIntervalTicks());
        assertTrue(DinosaurSpecies.TYRANNOSAURUS.stepHeight() > DinosaurSpecies.DODO.stepHeight());
        assertTrue(DinosaurSpecies.TYRANNOSAURUS.turnDegreesPerTick()
                < DinosaurSpecies.DODO.turnDegreesPerTick());
        assertEquals(DinosaurSpecies.Diet.CARNIVORE, DinosaurSpecies.TYRANNOSAURUS.diet());
        assertEquals(DinosaurSpecies.Diet.HERBIVORE, DinosaurSpecies.DODO.diet());
    }

    @Test
    void onlyAuthoredCombatSpeciesDefendThemselves() {
        assertTrue(DinosaurSpecies.TYRANNOSAURUS.combatCapable());
        assertTrue(DinosaurSpecies.VELOCIRAPTOR.combatCapable());
        assertFalse(DinosaurSpecies.TRICERATOPS.combatCapable());
        assertEquals(0.0D, DinosaurSpecies.TRICERATOPS.baseAttackDamage());
        assertTrue(DinosaurSpecies.ANKYLOSAURUS.combatCapable());
        assertTrue(DinosaurSpecies.SPINOSAURUS.combatCapable());
        assertTrue(!DinosaurSpecies.DODO.combatCapable());
        assertTrue(!DinosaurSpecies.BRACHIOSAURUS.combatCapable());
    }

    @Test
    void onlyApexPredatorsProactivelyAcquireHostiles() {
        for (DinosaurSpecies species : DinosaurSpecies.values()) {
            assertEquals(species == DinosaurSpecies.TYRANNOSAURUS
                            || species == DinosaurSpecies.SPINOSAURUS,
                    species.autoAttacksHostiles(), species.registryName());
        }
        assertTrue(DinosaurSpecies.VELOCIRAPTOR.combatCapable());
        assertFalse(DinosaurSpecies.VELOCIRAPTOR.autoAttacksHostiles());
        assertTrue(DinosaurSpecies.STEGOSAURUS.combatCapable());
        assertFalse(DinosaurSpecies.STEGOSAURUS.autoAttacksHostiles());
    }

    @Test
    void heavyweightDinosaursCannotBeBodyPushed() {
        assertTrue(DinosaurSpecies.TYRANNOSAURUS.heavyweight());
        assertTrue(DinosaurSpecies.STEGOSAURUS.heavyweight());
        assertTrue(DinosaurSpecies.PARASAUROLOPHUS.heavyweight());
        assertTrue(!DinosaurSpecies.DODO.heavyweight());
        assertTrue(!DinosaurSpecies.PTERANODON.heavyweight());
    }

    @Test
    void spinosaurusHasAUsableLandMountPace() {
        assertEquals(0.22D, DinosaurSpecies.SPINOSAURUS.baseMovementSpeed());
        assertTrue(DinosaurSpecies.SPINOSAURUS.baseMovementSpeed()
                > DinosaurSpecies.PTERANODON.baseMovementSpeed());
    }

    @Test
    void playableRosterContainsOnlyTheEightAuthoredDinosaurs() {
        assertEquals(8, DinosaurSpecies.playableSpecies().size());
        assertTrue(DinosaurSpecies.VELOCIRAPTOR.isPlayable());
        assertTrue(DinosaurSpecies.SPINOSAURUS.isPlayable());
        assertTrue(!DinosaurSpecies.BRACHIOSAURUS.isPlayable());
        assertTrue(!DinosaurSpecies.DILOPHOSAURUS.isPlayable());
        assertTrue(!DinosaurSpecies.ANKYLOSAURUS.isPlayable());
        assertTrue(!DinosaurSpecies.PACHYCEPHALOSAURUS.isPlayable());
    }

    @Test
    void velociraptorCanClimbTerrainAtChaseSpeed() {
        assertTrue(DinosaurSpecies.VELOCIRAPTOR.stepHeight() >= 1.0F);
        assertTrue(DinosaurSpecies.VELOCIRAPTOR.baseMovementSpeed()
                > DinosaurSpecies.DODO.baseMovementSpeed());
    }
}
