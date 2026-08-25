package com.primevalworks.world.entity;

import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.SoundKeyframeData;

import java.util.function.DoubleSupplier;

public final class DinosaurAnimationEvents {
    private static AnimationController.KeyframeEventHandler<FieldDodoEntity, SoundKeyframeData> footstepHandler =
            ignored -> { };
    private static DoubleSupplier unmountedSpinosaurusGaitSpeed = () -> 0.70D;

    private DinosaurAnimationEvents() {
    }

    public static void installFootstepHandler(
            AnimationController.KeyframeEventHandler<FieldDodoEntity, SoundKeyframeData> handler
    ) {
        footstepHandler = handler;
    }

    public static void installUnmountedSpinosaurusGaitSpeed(DoubleSupplier supplier) {
        unmountedSpinosaurusGaitSpeed = supplier;
    }

    public static void handleFootstep(KeyFrameEvent<FieldDodoEntity, SoundKeyframeData> event) {
        footstepHandler.handle(event);
    }

    public static float unmountedSpinosaurusGaitSpeed() {
        return (float)unmountedSpinosaurusGaitSpeed.getAsDouble();
    }
}
