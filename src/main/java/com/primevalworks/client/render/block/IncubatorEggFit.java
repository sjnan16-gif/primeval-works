package com.primevalworks.client.render.block;

public final class IncubatorEggFit {
    private IncubatorEggFit() {
    }

    public static float scaleForModelHeight(float modelHeight) {
        if (modelHeight <= 0.5F) return 1.60F;
        if (modelHeight <= 0.75F) return 1.48F;
        return 1.34F;
    }

    public static float centerYForModelHeight(float modelHeight) {
        if (modelHeight <= 0.5F) return 0.58F;
        if (modelHeight <= 0.75F) return 0.54F;
        return 0.49F;
    }
}
