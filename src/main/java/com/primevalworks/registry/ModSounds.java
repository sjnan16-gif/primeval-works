package com.primevalworks.registry;

import com.primevalworks.world.entity.DinosaurSpecies;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.EnumMap;
import java.util.Map;

public final class ModSounds {
    public static final SoundEvent EGG_HATCH = SoundEvents.SNIFFER_EGG_HATCH;
    public static final SoundEvent INCUBATOR_START = SoundEvents.SNIFFER_EGG_PLOP;
    public static final SoundEvent INCUBATOR_HATCH = SoundEvents.SNIFFER_EGG_HATCH;
    public static final SoundEvent PROCESS_COMPLETE = SoundEvents.SMITHING_TABLE_USE;

    private static final Map<DinosaurSpecies, DinosaurSounds> DINOSAUR_SOUNDS = createDinosaurSounds();

    private ModSounds() {
    }

    public static DinosaurSounds forSpecies(DinosaurSpecies species) {
        return DINOSAUR_SOUNDS.get(species);
    }

    private static Map<DinosaurSpecies, DinosaurSounds> createDinosaurSounds() {
        Map<DinosaurSpecies, DinosaurSounds> sounds = new EnumMap<>(DinosaurSpecies.class);
        DinosaurSounds apex = new DinosaurSounds(
                SoundEvents.RAVAGER_AMBIENT,
                SoundEvents.RAVAGER_ROAR,
                SoundEvents.RAVAGER_HURT,
                SoundEvents.RAVAGER_DEATH,
                SoundEvents.RAVAGER_ATTACK,
                SoundEvents.SNIFFER_EAT,
                SoundEvents.RAVAGER_STEP,
                SoundEvents.RAVAGER_STEP,
                SoundEvents.SNIFFER_IDLE,
                SoundEvents.RAVAGER_AMBIENT,
                SoundEvents.SNIFFER_DIGGING
        );
        DinosaurSounds hunter = new DinosaurSounds(
                SoundEvents.FOX_AMBIENT,
                SoundEvents.HOGLIN_AMBIENT,
                SoundEvents.FOX_HURT,
                SoundEvents.FOX_DEATH,
                SoundEvents.FOX_BITE,
                SoundEvents.FOX_EAT,
                SoundEvents.HOGLIN_STEP,
                SoundEvents.HOGLIN_STEP,
                SoundEvents.FOX_SLEEP,
                SoundEvents.FOX_AMBIENT,
                SoundEvents.SNIFFER_DIGGING
        );
        DinosaurSounds herbivore = new DinosaurSounds(
                SoundEvents.SNIFFER_IDLE,
                SoundEvents.CAMEL_AMBIENT,
                SoundEvents.SNIFFER_HURT,
                SoundEvents.SNIFFER_DEATH,
                SoundEvents.RAVAGER_ATTACK,
                SoundEvents.SNIFFER_EAT,
                SoundEvents.SNIFFER_STEP,
                SoundEvents.SNIFFER_STEP,
                SoundEvents.SNIFFER_IDLE,
                SoundEvents.SNIFFER_IDLE,
                SoundEvents.SNIFFER_DIGGING
        );
        DinosaurSounds pteranodon = new DinosaurSounds(
                SoundEvents.PHANTOM_AMBIENT,
                SoundEvents.PHANTOM_SWOOP,
                SoundEvents.PHANTOM_HURT,
                SoundEvents.PHANTOM_DEATH,
                SoundEvents.PHANTOM_BITE,
                SoundEvents.PARROT_EAT,
                SoundEvents.PHANTOM_FLAP,
                SoundEvents.PHANTOM_FLAP,
                SoundEvents.FOX_SLEEP,
                SoundEvents.PHANTOM_AMBIENT,
                SoundEvents.PARROT_FLY
        );
        DinosaurSounds dodo = new DinosaurSounds(
                SoundEvents.PARROT_AMBIENT,
                SoundEvents.PARROT_AMBIENT,
                SoundEvents.PARROT_HURT,
                SoundEvents.PARROT_DEATH,
                SoundEvents.PARROT_AMBIENT,
                SoundEvents.PARROT_EAT,
                SoundEvents.PARROT_STEP,
                SoundEvents.PARROT_STEP,
                SoundEvents.FOX_SLEEP,
                SoundEvents.PARROT_AMBIENT,
                SoundEvents.PARROT_FLY
        );

        for (DinosaurSpecies species : DinosaurSpecies.values()) {
            sounds.put(species, switch (species) {
                case TYRANNOSAURUS, SPINOSAURUS -> apex;
                case DILOPHOSAURUS, VELOCIRAPTOR -> hunter;
                case PTERANODON -> pteranodon;
                case DODO -> dodo;
                default -> herbivore;
            });
        }
        return Map.copyOf(sounds);
    }

    public record DinosaurSounds(
            SoundEvent ambient,
            SoundEvent alert,
            SoundEvent hurt,
            SoundEvent death,
            SoundEvent attack,
            SoundEvent eat,
            SoundEvent step,
            SoundEvent runStep,
            SoundEvent sleep,
            SoundEvent wake,
            SoundEvent work
    ) {
    }
}
