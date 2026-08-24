package com.primevalworks.world.work;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityRoutingTest {
    @Test
    void higherPriorityAlwaysWins() {
        List<Candidate> ordered = PriorityRouting.order(
                List.of(
                        new Candidate("near low", 0, 1.0D),
                        new Candidate("far urgent", 3, 30.0D),
                        new Candidate("near high", 2, 2.0D)
                ),
                Candidate::priority,
                Candidate::distance,
                true
        );

        assertEquals(List.of("far urgent", "near high", "near low"), names(ordered));
    }

    @Test
    void manualOrderIsStableWithinEqualPriority() {
        List<Candidate> ordered = PriorityRouting.order(
                List.of(
                        new Candidate("first", 2, 40.0D),
                        new Candidate("second", 2, 1.0D),
                        new Candidate("third", 2, 4.0D)
                ),
                Candidate::priority,
                Candidate::distance,
                false
        );

        assertEquals(List.of("first", "second", "third"), names(ordered));
    }

    @Test
    void nearestPolicyOnlyReordersEqualPriorities() {
        List<Candidate> ordered = PriorityRouting.order(
                List.of(
                        new Candidate("far normal", 1, 12.0D),
                        new Candidate("near normal", 1, 2.0D),
                        new Candidate("far high", 2, 50.0D)
                ),
                Candidate::priority,
                Candidate::distance,
                true
        );

        assertEquals(List.of("far high", "near normal", "far normal"), names(ordered));
    }

    private static List<String> names(List<Candidate> candidates) {
        return candidates.stream().map(Candidate::name).toList();
    }

    private record Candidate(String name, int priority, double distance) {
    }
}
