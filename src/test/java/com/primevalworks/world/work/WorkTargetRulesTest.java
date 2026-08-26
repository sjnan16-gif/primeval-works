package com.primevalworks.world.work;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorkTargetRulesTest {
    @Test
    void multiTargetOrdersAcceptEveryCountUpToTheirLimit() {
        assertFalse(WorkTargetRules.acceptsNonEmptyTargetCount(0, 8));
        assertTrue(WorkTargetRules.acceptsNonEmptyTargetCount(1, 8));
        assertTrue(WorkTargetRules.acceptsNonEmptyTargetCount(2, 8));
        assertTrue(WorkTargetRules.acceptsNonEmptyTargetCount(8, 8));
        assertFalse(WorkTargetRules.acceptsNonEmptyTargetCount(9, 8));
    }

    @Test
    void transportCannotFeedAnInventoryBackIntoItself() {
        assertTrue(WorkTargetRules.routesDoNotOverlap(List.of("source_a", "source_b"), List.of("destination")));
        assertFalse(WorkTargetRules.routesDoNotOverlap(List.of("source_a", "shared"), List.of("shared", "destination")));
    }
}
