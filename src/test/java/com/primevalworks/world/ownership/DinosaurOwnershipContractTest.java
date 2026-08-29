package com.primevalworks.world.ownership;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DinosaurOwnershipContractTest {
    @Test
    void depotHasTwentyFiveTwelveDinosaurPages() {
        assertEquals(12, DinosaurOwnership.DEPOT_PAGE_SIZE);
        assertEquals(25, DinosaurOwnership.MAX_DEPOT_PAGES);
        assertEquals(300, DinosaurOwnership.MAX_OWNED);
    }
}
