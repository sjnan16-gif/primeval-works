package com.primevalworks.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

final class PrimevalUiSounds {
    private static final Set<Screen> OPEN_SCREENS = Collections.newSetFromMap(new WeakHashMap<>());

    private PrimevalUiSounds() {
    }

    static void open(Screen screen) {
        if (OPEN_SCREENS.add(screen)) play(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.18F, 0.18F);
    }

    static void close(Screen screen) {
        OPEN_SCREENS.remove(screen);
        play(SoundEvents.WOODEN_BUTTON_CLICK_OFF, 0.92F, 0.16F);
    }

    static void click() {
        click(1.0F);
    }

    static void click(float pitch) {
        play(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.24F);
    }

    static void denied() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), 0.58F, 0.28F);
    }

    static void upgrade() {
        play(SoundEvents.PLAYER_LEVELUP, 1.18F, 0.36F);
        play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.92F, 0.32F);
    }

    private static void play(SoundEvent sound, float pitch, float volume) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getSoundManager() == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
