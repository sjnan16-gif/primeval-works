package com.primevalworks.world.work;

public enum DinoFieldSpecialtyProfile {
    TYRANNOSAURUS(Role.QUARRY, 1),
    TRICERATOPS(Role.NONE, -1),
    BRACHIOSAURUS(Role.NONE, -1),
    DILOPHOSAURUS(Role.NONE, -1),
    VELOCIRAPTOR(Role.COLLECT, 0),
    STEGOSAURUS(Role.NONE, -1),
    PARASAUROLOPHUS(Role.LUMBER, 3),
    ANKYLOSAURUS(Role.NONE, -1),
    PTERANODON(Role.NONE, -1),
    DODO(Role.HARVEST, 4),
    SPINOSAURUS(Role.QUARRY, 2),
    PACHYCEPHALOSAURUS(Role.NONE, -1);

    private final Role role;
    private final int sourceJobIndex;

    DinoFieldSpecialtyProfile(Role role, int sourceJobIndex) {
        this.role = role;
        this.sourceJobIndex = sourceJobIndex;
    }

    public Role role() {
        return role;
    }

    public int sourceJobIndex() {
        return sourceJobIndex;
    }

    public boolean eligible() {
        return role != Role.NONE;
    }

    public enum Role {
        NONE,
        QUARRY,
        LUMBER,
        HARVEST,
        COLLECT
    }
}
