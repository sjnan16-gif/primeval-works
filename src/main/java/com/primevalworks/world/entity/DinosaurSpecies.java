package com.primevalworks.world.entity;

import java.util.List;

public enum DinosaurSpecies {
    TYRANNOSAURUS("tyrannosaurus", 2.03F, 3.25F, 1.25F, 8.5F, 320, 2.40F, Diet.CARNIVORE, 180.0D, 20.0D, 0.23D),
    TRICERATOPS("triceratops", 1.875F, 3.375F, 1.00F, 7.5F, 480, 1.55F, Diet.HERBIVORE, 80.0D, 0.0D, 0.24D),
    BRACHIOSAURUS("brachiosaurus", 1.50F, 3.20F, 1.50F, 5.5F, 280, 2.60F, Diet.HERBIVORE, 130.0D, 5.0D, 0.18D),
    DILOPHOSAURUS("dilophosaurus", 0.80F, 1.40F, 0.85F, 11.0F, 620, 1.15F, Diet.CARNIVORE, 30.0D, 6.0D, 0.24D),
    VELOCIRAPTOR("velociraptor", 0.77F, 1.55F, 1.15F, 14.0F, 680, 1.00F, Diet.CARNIVORE, 28.0D, 7.0D, 0.30D),
    STEGOSAURUS("stegosaurus", 1.75F, 2.88F, 1.05F, 6.5F, 440, 1.70F, Diet.HERBIVORE, 85.0D, 11.0D, 0.24D),
    PARASAUROLOPHUS("parasaurolophus", 1.16F, 3.31F, 1.10F, 8.5F, 500, 1.50F, Diet.HERBIVORE, 52.0D, 4.0D, 0.24D),
    ANKYLOSAURUS("ankylosaurus", 1.20F, 1.25F, 0.90F, 6.5F, 520, 1.40F, Diet.HERBIVORE, 95.0D, 10.0D, 0.24D),
    PTERANODON("pteranodon", 1.35F, 1.25F, 0.85F, 11.0F, 640, 1.05F, Diet.CARNIVORE, 34.0D, 4.0D, 0.18D),
    DODO("field_dodo", 0.88F, 1.56F, 0.65F, 13.0F, 840, 0.80F, Diet.HERBIVORE, 16.0D, 2.0D, 0.26D),
    SPINOSAURUS("spinosaurus", 2.03F, 5.05F, 1.45F, 7.0F, 300, 2.70F, Diet.CARNIVORE, 200.0D, 19.0D, 0.22D),
    PACHYCEPHALOSAURUS("pachycephalosaurus", 0.80F, 1.40F, 0.85F, 10.5F, 650, 1.05F, Diet.HERBIVORE, 42.0D, 8.0D, 0.26D);

    private final String registryName;
    private final float collisionWidth;
    private final float collisionHeight;
    private final float stepHeight;
    private final float turnDegreesPerTick;
    private final int hungerDrainIntervalTicks;
    private final float appetite;
    private final Diet diet;
    private final double baseHealth;
    private final double baseAttackDamage;
    private final double baseMovementSpeed;
    private static final List<DinosaurSpecies> PLAYABLE = List.of(
            TYRANNOSAURUS,
            TRICERATOPS,
            VELOCIRAPTOR,
            STEGOSAURUS,
            PARASAUROLOPHUS,
            PTERANODON,
            DODO,
            SPINOSAURUS
    );

    DinosaurSpecies(
            String registryName,
            float collisionWidth,
            float collisionHeight,
            float stepHeight,
            float turnDegreesPerTick,
            int hungerDrainIntervalTicks,
            float appetite,
            Diet diet,
            double baseHealth,
            double baseAttackDamage,
            double baseMovementSpeed
    ) {
        this.registryName = registryName;
        this.collisionWidth = collisionWidth;
        this.collisionHeight = collisionHeight;
        this.stepHeight = stepHeight;
        this.turnDegreesPerTick = turnDegreesPerTick;
        this.hungerDrainIntervalTicks = hungerDrainIntervalTicks;
        this.appetite = appetite;
        this.diet = diet;
        this.baseHealth = baseHealth;
        this.baseAttackDamage = baseAttackDamage;
        this.baseMovementSpeed = baseMovementSpeed;
    }

    public static DinosaurSpecies byRegistryName(String registryName) {
        for (DinosaurSpecies species : values()) {
            if (species.registryName.equals(registryName)) {
                return species;
            }
        }
        return DODO;
    }

    public static List<DinosaurSpecies> playableSpecies() {
        return PLAYABLE;
    }

    public boolean isPlayable() {
        return PLAYABLE.contains(this);
    }

    public String registryName() {
        return registryName;
    }

    public float collisionWidth() {
        return collisionWidth;
    }

    public float collisionHeight() {
        return collisionHeight;
    }

    public float stepHeight() {
        return stepHeight;
    }

    public float turnDegreesPerTick() {
        return turnDegreesPerTick;
    }

    public int hungerDrainIntervalTicks() {
        return hungerDrainIntervalTicks;
    }

    public float appetite() {
        return appetite;
    }

    public Diet diet() {
        return diet;
    }

    public double baseHealth() {
        return baseHealth;
    }

    public double baseAttackDamage() {
        return baseAttackDamage;
    }

    public double baseMovementSpeed() {
        return baseMovementSpeed;
    }

    public boolean combatCapable() {
        return switch (this) {
            case TYRANNOSAURUS, DILOPHOSAURUS, VELOCIRAPTOR,
                    STEGOSAURUS, ANKYLOSAURUS, SPINOSAURUS, PACHYCEPHALOSAURUS -> true;
            default -> false;
        };
    }

    public boolean autoAttacksHostiles() {
        return this == TYRANNOSAURUS || this == SPINOSAURUS;
    }

    public boolean heavyweight() {
        return switch (this) {
            case TYRANNOSAURUS, TRICERATOPS, BRACHIOSAURUS, STEGOSAURUS,
                    PARASAUROLOPHUS, ANKYLOSAURUS, SPINOSAURUS -> true;
            default -> false;
        };
    }

    public float workReach() {
        return switch (this) {
            case TYRANNOSAURUS -> 3.0F;
            case SPINOSAURUS -> 3.6F;
            case BRACHIOSAURUS -> 3.1F;
            case STEGOSAURUS -> 2.35F;
            case PARASAUROLOPHUS -> 2.15F;
            case TRICERATOPS, ANKYLOSAURUS -> 1.85F;
            case PTERANODON -> 1.25F;
            default -> 1.0F;
        };
    }

    public float fieldWorkReach() {
        return switch (this) {
            case SPINOSAURUS -> 7.0F;
            case BRACHIOSAURUS -> 7.5F;
            case TYRANNOSAURUS -> 6.2F;
            case STEGOSAURUS -> 4.8F;
            case PARASAUROLOPHUS -> 4.3F;
            case TRICERATOPS, ANKYLOSAURUS -> 4.0F;
            case PTERANODON -> 3.8F;
            case VELOCIRAPTOR -> 3.4F;
            default -> 2.8F;
        };
    }

    public String passiveTitle() {
        return switch (this) {
            case TYRANNOSAURUS -> "APEX FURNACE";
            case TRICERATOPS -> "LOAD BRACE";
            case BRACHIOSAURUS -> "HIGH REACH";
            case DILOPHOSAURUS -> "EMBER VENOM";
            case VELOCIRAPTOR -> "PURSUIT INSTINCT";
            case STEGOSAURUS -> "EMBER PLATES";
            case PARASAUROLOPHUS -> "CALMING CALL";
            case ANKYLOSAURUS -> "STONEBREAKER";
            case PTERANODON -> "SKY ROUTES";
            case DODO -> "FORAGER'S EYE";
            case SPINOSAURUS -> "CURRENT DYNAMO";
            case PACHYCEPHALOSAURUS -> "QUEUE RAM";
        };
    }

    public String passiveSummary() {
        return switch (this) {
            case TYRANNOSAURUS -> "Dominates fire work";
            case TRICERATOPS -> "Carries heavy loads";
            case BRACHIOSAURUS -> "Works over obstacles";
            case DILOPHOSAURUS -> "Fast heat tending";
            case VELOCIRAPTOR -> "Builds speed while running";
            case STEGOSAURUS -> "Keeps heat steady";
            case PARASAUROLOPHUS -> "Raises nearby mood";
            case ANKYLOSAURUS -> "Excels on expeditions";
            case PTERANODON -> "Flies long cargo routes";
            case DODO -> "Finds extra supplies";
            case SPINOSAURUS -> "Boosts turbine output";
            case PACHYCEPHALOSAURUS -> "Fast crafting cycles";
        };
    }

    public String passiveDetail() {
        return switch (this) {
            case TYRANNOSAURUS -> "Its size and body heat make furnace work faster.";
            case TRICERATOPS -> "Heavy cargo has less effect on its carrying speed.";
            case BRACHIOSAURUS -> "Its long reach keeps large workstations clear.";
            case DILOPHOSAURUS -> "Short, focused bursts speed up fire work.";
            case VELOCIRAPTOR -> "Every uninterrupted stride builds momentum and raises its top speed.";
            case STEGOSAURUS -> "Its plates hold heat, shortening fire work cycles.";
            case PARASAUROLOPHUS -> "Calming calls slow mood loss for nearby workers.";
            case ANKYLOSAURUS -> "Its armor lowers expedition injury risk.";
            case PTERANODON -> "Long transport routes switch to direct flight.";
            case DODO -> "Expeditions return with a small extra reward chance.";
            case SPINOSAURUS -> "Water-trained strength raises turbine generation.";
            case PACHYCEPHALOSAURUS -> "Repeated crafting queues lose less setup time.";
        };
    }

    public int passiveColor() {
        return switch (this) {
            case TYRANNOSAURUS -> 0xFFC54B2D;
            case TRICERATOPS -> 0xFF8A684A;
            case VELOCIRAPTOR -> 0xFF4D8A62;
            case STEGOSAURUS, DILOPHOSAURUS -> 0xFFD47B3B;
            case PARASAUROLOPHUS -> 0xFF6A8FC2;
            case PTERANODON -> 0xFF61A9BE;
            case SPINOSAURUS -> 0xFFD09A16;
            case DODO -> 0xFF6F984B;
            default -> 0xFF75679A;
        };
    }

    public float passiveWorkSpeedMultiplier(int jobIndex) {
        return passiveWorkSpeedMultiplier(jobIndex, 1.0F);
    }

    public float passiveWorkSpeedMultiplier(int jobIndex, float passiveStrength) {
        float authored = switch (this) {
            case TYRANNOSAURUS -> jobIndex == 1 ? 1.12F : 1.0F;
            case TRICERATOPS -> jobIndex == 0 ? 1.10F : 1.0F;
            case DILOPHOSAURUS -> jobIndex == 1 ? 1.10F : 1.0F;
            case STEGOSAURUS -> jobIndex == 1 ? 1.12F : 1.0F;
            case PTERANODON -> jobIndex == 0 ? 1.06F : 1.0F;
            case SPINOSAURUS -> jobIndex == 2 ? 1.15F : 1.0F;
            case PACHYCEPHALOSAURUS -> jobIndex == 3 ? 1.10F : 1.0F;
            default -> 1.0F;
        };
        return 1.0F + (authored - 1.0F) * Math.max(0.0F, passiveStrength);
    }

    public enum Diet {
        HERBIVORE,
        CARNIVORE,
        OMNIVORE
    }
}
