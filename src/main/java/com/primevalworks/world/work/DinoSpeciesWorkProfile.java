package com.primevalworks.world.work;

import java.util.Arrays;

public enum DinoSpeciesWorkProfile {
    TYRANNOSAURUS(1, 4, 1, 1, 4),
    TRICERATOPS(3, 1, 1, 2, 3),
    BRACHIOSAURUS(4, 1, 2, 1, 3),
    DILOPHOSAURUS(2, 4, 1, 2, 2),
    VELOCIRAPTOR(4, 1, 1, 2, 2),
    STEGOSAURUS(1, 3, 1, 1, 3),
    PARASAUROLOPHUS(1, 1, 3, 3, 1),
    ANKYLOSAURUS(2, 1, 2, 2, 4),
    PTERANODON(3, 1, 2, 1, 2),
    DODO(2, 2, 1, 2, 4),
    SPINOSAURUS(1, 2, 4, 1, 4),
    PACHYCEPHALOSAURUS(2, 1, 2, 4, 3);

    private final int[] specialtyStars;

    DinoSpeciesWorkProfile(int transport, int fire, int energy, int crafting, int gathering) {
        specialtyStars = new int[]{transport, fire, energy, crafting, gathering};
    }

    public int stars(int jobIndex) {
        return specialtyStars[Math.max(0, Math.min(4, jobIndex))];
    }

    public int[] specialtyStars() {
        return Arrays.copyOf(specialtyStars, specialtyStars.length);
    }
}
