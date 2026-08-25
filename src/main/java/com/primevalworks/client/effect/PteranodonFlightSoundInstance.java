package com.primevalworks.client.effect;

import com.primevalworks.config.PrimevalConfig;
import com.primevalworks.config.PrimevalTuning;
import com.primevalworks.world.entity.DinosaurSpecies;
import com.primevalworks.world.entity.FieldDodoEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

final class PteranodonFlightSoundInstance extends AbstractTickableSoundInstance {
    private final LocalPlayer rider;
    private final FieldDodoEntity pteranodon;
    private float presence;

    PteranodonFlightSoundInstance(LocalPlayer rider, FieldDodoEntity pteranodon) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.rider = rider;
        this.pteranodon = pteranodon;
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.01F;
        this.pitch = 0.82F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        boolean flying = PrimevalConfig.CLIENT.pteranodonWind.get()
                && !rider.isRemoved()
                && !pteranodon.isRemoved()
                && rider.getVehicle() == pteranodon
                && pteranodon.getSpecies() == DinosaurSpecies.PTERANODON
                && pteranodon.isPteranodonAirborne();
        presence = Mth.approach(presence, flying ? 1.0F : 0.0F, flying ? 0.08F : 0.14F);
        if (!flying && presence <= 0.001F) {
            stop();
            return;
        }

        x = pteranodon.getX();
        y = pteranodon.getY() + pteranodon.getBbHeight() * 0.55D;
        z = pteranodon.getZ();
        float speed = Mth.clamp(pteranodon.getPteranodonFlightSpeed()
                / (1.72F * (float)PrimevalTuning.server().pteranodonFlightSpeed()), 0.0F, 1.0F);
        float targetVolume = presence * (0.055F + 0.82F * speed * speed)
                * PrimevalConfig.CLIENT.pteranodonWindVolume.get().floatValue();
        volume = Mth.lerp(0.24F, volume, targetVolume);
        pitch = Mth.lerp(0.20F, pitch, 0.76F + speed * 0.44F);
    }
}
