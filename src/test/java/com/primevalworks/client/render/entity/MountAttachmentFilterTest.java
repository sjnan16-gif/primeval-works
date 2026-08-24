package com.primevalworks.client.render.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MountAttachmentFilterTest {
    @Test
    void groundAttachmentHasNoArtificialCatchUp() {
        assertEquals(1.0D, MountAttachmentFilter.alpha(false, 1.0D / 60.0D));
    }

    @Test
    void aquaticAttachmentUsesAStableLowLatencyResponse() {
        double alpha = MountAttachmentFilter.alpha(true, 1.0D / 60.0D);
        assertTrue(alpha > 0.45D && alpha < 0.55D);
    }
}
