package com.primevalworks.registry;

import com.primevalworks.PrimevalWorks;
import com.primevalworks.world.entity.DinosaurSpecies;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModSounds {
    private static final boolean SOUND_ASSETS_READY = false;
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, PrimevalWorks.MOD_ID);

    private static final Map<DinosaurSpecies, DinosaurSounds> DINOSAUR_SOUNDS = new EnumMap<>(DinosaurSpecies.class);

    public static final DeferredHolder<SoundEvent, SoundEvent> EGG_PLACE = register("block.egg.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGG_HATCH = register("block.egg.hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCUBATOR_START = register("block.incubator.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCUBATOR_LOOP = register("block.incubator.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCUBATOR_HATCH = register("block.incubator.hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMAND_TABLE_OPEN = register("block.command_table.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMAND_TABLE_ASSIGN = register("block.command_table.assign");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMAND_TABLE_ERROR = register("block.command_table.error");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOOD_BOX_OPEN = register("block.food_box.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_TURBINE_LOOP = register("block.wind_turbine.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_TURBINE_LOOP = register("block.water_turbine.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENERGY_PULSE = register("block.energy.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> PROCESSOR_LOOP = register("block.processor.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> PROCESS_COMPLETE = register("block.processor.complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> DART_TURRET_FIRE = register("block.dart_turret.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGIC_TURRET_CHARGE = register("block.magic_turret.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGIC_TURRET_FIRE = register("block.magic_turret.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORK_PICKUP = register("work.pickup");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORK_DEPOSIT = register("work.deposit");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORK_CRAFT = register("work.craft");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORK_SMELT = register("work.smelt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORK_GATHER = register("work.gather");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_OPEN = register("ui.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLOSE = register("ui.close");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_HOVER = register("ui.hover");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLICK = register("ui.click");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_WARNING = register("ui.warning");

    static {
        for (DinosaurSpecies species : DinosaurSpecies.values()) {
            String root = "entity." + species.registryName() + ".";
            DINOSAUR_SOUNDS.put(species, new DinosaurSounds(
                    register(root + "ambient"),
                    register(root + "alert"),
                    register(root + "hurt"),
                    register(root + "death"),
                    register(root + "attack"),
                    register(root + "eat"),
                    register(root + "step"),
                    register(root + "run_step"),
                    register(root + "sleep"),
                    register(root + "wake"),
                    register(root + "work")
            ));
        }
    }

    private ModSounds() {
    }

    public static DinosaurSounds forSpecies(DinosaurSpecies species) {
        return DINOSAUR_SOUNDS.get(species);
    }

    public static boolean areAssetsReady() {
        return SOUND_ASSETS_READY;
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(PrimevalWorks.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public record DinosaurSounds(
            DeferredHolder<SoundEvent, SoundEvent> ambient,
            DeferredHolder<SoundEvent, SoundEvent> alert,
            DeferredHolder<SoundEvent, SoundEvent> hurt,
            DeferredHolder<SoundEvent, SoundEvent> death,
            DeferredHolder<SoundEvent, SoundEvent> attack,
            DeferredHolder<SoundEvent, SoundEvent> eat,
            DeferredHolder<SoundEvent, SoundEvent> step,
            DeferredHolder<SoundEvent, SoundEvent> runStep,
            DeferredHolder<SoundEvent, SoundEvent> sleep,
            DeferredHolder<SoundEvent, SoundEvent> wake,
            DeferredHolder<SoundEvent, SoundEvent> work
    ) {
    }
}
