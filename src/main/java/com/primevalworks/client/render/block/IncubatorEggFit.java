package com.primevalworks.client.render.block;

public final class IncubatorEggFit {
    private IncubatorEggFit() {
    }

    public static float scaleForModelHeight(float modelHeight) {
        return modelHeight < 0.8F ? 0.86F : 0.60F;
    }

    public static float centerYForModelHeight(float modelHeight) {
        return modelHeight < 0.8F ? 0.755F : 0.61F;
    }
}
