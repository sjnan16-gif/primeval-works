package com.primevalworks.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CommandTableUiLayoutTest {
    @Test
    void currentDepotSpriteUsesCompactRowsWithoutCumulativeDrift() {
        assertEquals(26, CommandTableScreen.DEPOT_PREVIEW_X_STRIDE);
        assertEquals(27, CommandTableScreen.DEPOT_PREVIEW_Y_STRIDE);
    }

    @Test
    void authoredControlButtonLabelsKeepTheirIndividualOffsets() {
        assertEquals(-4.0F, CommandTableScreen.STORE_ALL_TEXT_OFFSET_X);
        assertEquals(-2.0F, CommandTableScreen.RECALL_TEXT_OFFSET_X);
        assertEquals(-1.0F, CommandTableScreen.ENERGY_TEXT_OFFSET_X);
        assertEquals(1.5F, CommandTableScreen.DEPOT_TEXT_OFFSET_X);
        assertEquals(1.0F, CommandTableScreen.ACTION_TEXT_OFFSET_Y);
    }
}
