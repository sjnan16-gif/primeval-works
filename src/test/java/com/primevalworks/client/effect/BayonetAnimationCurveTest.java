package com.primevalworks.client.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BayonetAnimationCurveTest {
    @Test
    void stabExtendsHoldsAndReturnsToItsReverseGrip() {
        BayonetAnimationCurve.Sample rest = BayonetAnimationCurve.sample(0.0F);
        BayonetAnimationCurve.Sample contact = BayonetAnimationCurve.sample(0.24F);
        BayonetAnimationCurve.Sample pullback = BayonetAnimationCurve.sample(0.72F);
        BayonetAnimationCurve.Sample finished = BayonetAnimationCurve.sample(1.0F);

        assertEquals(0.0F, rest.extension(), 0.0001F);
        assertEquals(1.0F, contact.extension(), 0.0001F);
        assertEquals(180.0F, contact.orientationDegrees(), 0.0001F);
        assertTrue(pullback.extension() < contact.extension());
        assertTrue(pullback.orientationDegrees() > 180.0F);
        assertEquals(0.0F, finished.extension(), 0.0001F);
        assertEquals(720.0F, finished.orientationDegrees(), 0.001F);
    }
}
