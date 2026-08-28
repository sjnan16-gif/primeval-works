package com.primevalworks.client.render.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IncubatorEggFitTest {
    @Test
    void everyEggFillsTheChamberWithoutLargeEggsClipping() {
        assertEquals(1.60F, IncubatorEggFit.scaleForModelHeight(0.5F));
        assertEquals(1.48F, IncubatorEggFit.scaleForModelHeight(0.75F));
        assertEquals(1.34F, IncubatorEggFit.scaleForModelHeight(1.0F));
        assertTrue(IncubatorEggFit.scaleForModelHeight(0.5F)
                > IncubatorEggFit.scaleForModelHeight(1.0F));
        assertEquals(0.58F, IncubatorEggFit.centerYForModelHeight(0.5F));
        assertEquals(0.54F, IncubatorEggFit.centerYForModelHeight(0.75F));
        assertEquals(0.49F, IncubatorEggFit.centerYForModelHeight(1.0F));
    }
}
