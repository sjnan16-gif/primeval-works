package com.primevalworks.client.render.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IncubatorEggFitTest {
    @Test
    void everyEggFillsTheChamberWithoutLargeEggsClipping() {
        assertEquals(0.86F, IncubatorEggFit.scaleForModelHeight(0.6875F));
        assertEquals(0.60F, IncubatorEggFit.scaleForModelHeight(1.0F));
        assertTrue(IncubatorEggFit.scaleForModelHeight(0.6875F)
                > IncubatorEggFit.scaleForModelHeight(1.0F));
        assertEquals(0.755F, IncubatorEggFit.centerYForModelHeight(0.6875F));
        assertEquals(0.61F, IncubatorEggFit.centerYForModelHeight(1.0F));
    }
}
