package com.primevalworks.client.effect;

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
        this.volume = 0.0F;
        this.pitch = 0.82F;
    }

    @Override
    public void tick() {
        boolean flying = !rider.isRemoved()
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
        float speed = Mth.clamp(pteranodon.getPteranodonFlightSpeed() / 1.72F, 0.0F, 1.0F);
        float airflow = 0.10F + 0.54F * speed * speed;
        volume = presence * airflow;
        pitch = 0.80F + speed * 0.30F;
    }
}
