package com.primevalworks.client.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DinosaurPreviewBoundsTest {
    @Test
    void spinosaurusUsesItsAuthoredLongBodyInEveryPreview() {
        DinosaurPreviewBounds bounds = DinosaurPreviewBounds.forAsset("spino", 1.0F);

        assertEquals(2.03F, bounds.width());
        assertEquals(5.05F, bounds.height());
        assertEquals(10.06F, bounds.depth());
    }

    @Test
    void velociraptorUsesItsAuthoredBoundsInsteadOfTheDodoPlaceholder() {
        DinosaurPreviewBounds bounds = DinosaurPreviewBounds.forAsset("velociraptor", 1.0F);

        assertEquals(0.77F, bounds.width());
        assertEquals(1.55F, bounds.height());
        assertEquals(3.47F, bounds.depth());
    }
}
