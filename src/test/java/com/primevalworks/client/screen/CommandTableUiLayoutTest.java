package com.primevalworks.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CommandTableUiLayoutTest {
    @Test
    void currentDepotSpriteUsesCompactRowsWithoutCumulativeDrift() {
        assertEquals(26, CommandTableTestScreen.DEPOT_PREVIEW_X_STRIDE);
        assertEquals(27, CommandTableTestScreen.DEPOT_PREVIEW_Y_STRIDE);
    }
}
