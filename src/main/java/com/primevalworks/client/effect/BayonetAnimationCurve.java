package com.primevalworks.client.effect;

public final class BayonetAnimationCurve {
    private BayonetAnimationCurve() {
    }

    public static Sample sample(float swingProgress) {
        float progress = clamp(swingProgress);
        if (progress <= 0.18F) {
            float attack = progress / 0.18F;
            float eased = outBack(attack);
            return new Sample(eased, smoothStep(attack) * 180.0F, 0.0F);
        }
        if (progress <= 0.46F) {
            return new Sample(1.0F, 180.0F, 0.0F);
        }
        float retract = smootherStep((progress - 0.46F) / 0.54F);
        return new Sample(1.0F - retract, 180.0F + retract * 540.0F, retract);
    }

    private static float outBack(float value) {
        float t = clamp(value) - 1.0F;
        return 1.0F + 2.70158F * t * t * t + 1.70158F * t * t;
    }

    private static float smoothStep(float value) {
        float t = clamp(value);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float smootherStep(float value) {
        float t = clamp(value);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public record Sample(float extension, float orientationDegrees, float retract) {
    }
}
