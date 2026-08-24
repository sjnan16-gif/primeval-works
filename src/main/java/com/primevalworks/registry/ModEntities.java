package com.primevalworks.registry;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.FieldDodoEntity;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.DartProjectileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, PrimevalWorks.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> TYRANNOSAURUS = registerDinosaur(DinosaurSpecies.TYRANNOSAURUS);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> TRICERATOPS = registerDinosaur(DinosaurSpecies.TRICERATOPS);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> BRACHIOSAURUS = registerDinosaur(DinosaurSpecies.BRACHIOSAURUS);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> DILOPHOSAURUS = registerDinosaur(DinosaurSpecies.DILOPHOSAURUS);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> VELOCIRAPTOR = registerDinosaur(DinosaurSpecies.VELOCIRAPTOR);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> STEGOSAURUS = registerDinosaur(DinosaurSpecies.STEGOSAURUS);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> PARASAUROLOPHUS = registerDinosaur(DinosaurSpecies.PARASAUROLOPHUS);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> ANKYLOSAURUS = registerDinosaur(DinosaurSpecies.ANKYLOSAURUS);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> PTERANODON = registerDinosaur(DinosaurSpecies.PTERANODON);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> FIELD_DODO = registerDinosaur(DinosaurSpecies.DODO);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> SPINOSAURUS = registerDinosaur(DinosaurSpecies.SPINOSAURUS);
    public static final DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> PACHYCEPHALOSAURUS = registerDinosaur(DinosaurSpecies.PACHYCEPHALOSAURUS);
    public static final DeferredHolder<EntityType<?>, EntityType<DartProjectileEntity>> DART_PROJECTILE =
            ENTITIES.register("dart_projectile", () -> EntityType.Builder.<DartProjectileEntity>of(
                            DartProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, "dart_projectile"))));

    public static final List<DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>>> DINOSAURS = List.of(
            TYRANNOSAURUS, TRICERATOPS, BRACHIOSAURUS, DILOPHOSAURUS, VELOCIRAPTOR, STEGOSAURUS,
            PARASAUROLOPHUS, ANKYLOSAURUS, PTERANODON, FIELD_DODO, SPINOSAURUS, PACHYCEPHALOSAURUS
    );

    private static DeferredHolder<EntityType<?>, EntityType<FieldDodoEntity>> registerDinosaur(DinosaurSpecies species) {
        String name = species.registryName();
        return ENTITIES.register(
            name,
            () -> EntityType.Builder.of(FieldDodoEntity::new, MobCategory.CREATURE)
                    .sized(species.collisionWidth(), species.collisionHeight())
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, name)
                    ))
        );
    }

    private ModEntities() {
    }

    public static EntityType<FieldDodoEntity> typeFor(DinosaurSpecies species) {
        return switch (species) {
            case TYRANNOSAURUS -> TYRANNOSAURUS.get();
            case TRICERATOPS -> TRICERATOPS.get();
            case BRACHIOSAURUS -> BRACHIOSAURUS.get();
            case DILOPHOSAURUS -> DILOPHOSAURUS.get();
            case VELOCIRAPTOR -> VELOCIRAPTOR.get();
            case STEGOSAURUS -> STEGOSAURUS.get();
            case PARASAUROLOPHUS -> PARASAUROLOPHUS.get();
            case ANKYLOSAURUS -> ANKYLOSAURUS.get();
            case PTERANODON -> PTERANODON.get();
            case DODO -> FIELD_DODO.get();
            case SPINOSAURUS -> SPINOSAURUS.get();
            case PACHYCEPHALOSAURUS -> PACHYCEPHALOSAURUS.get();
        };
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.<EntityAttributeCreationEvent>addListener(event -> {
            DinosaurSpecies[] species = DinosaurSpecies.values();
            for (int index = 0; index < DINOSAURS.size(); index++) {
                event.put(DINOSAURS.get(index).get(), attributesFor(species[index]));
            }
        });
    }

    private static AttributeSupplier attributesFor(DinosaurSpecies species) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, species.baseHealth())
                .add(Attributes.MOVEMENT_SPEED, species.baseMovementSpeed())
                .add(Attributes.ATTACK_DAMAGE, species.baseAttackDamage())
                .add(Attributes.KNOCKBACK_RESISTANCE, species.heavyweight() ? 1.0D : 0.0D)
                .add(Attributes.SCALE, 1.0D)
                .add(Attributes.FOLLOW_RANGE,
                        species == DinosaurSpecies.TYRANNOSAURUS || species == DinosaurSpecies.SPINOSAURUS
                                ? 32.0D : 18.0D)
                .build();
    }
}
