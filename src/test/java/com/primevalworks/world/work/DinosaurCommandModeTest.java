package com.primevalworks.world.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DinosaurCommandModeTest {
    @Test
    void normalCommandCycleRemainsHomeStayFollowHome() {
        assertEquals(DinosaurCommandMode.STAY, DinosaurCommandMode.HOME.next(true));
        assertEquals(DinosaurCommandMode.FOLLOW, DinosaurCommandMode.STAY.next(true));
        assertEquals(DinosaurCommandMode.HOME, DinosaurCommandMode.FOLLOW.next(true));
    }

    @Test
    void fullFollowerCrewSkipsFollowWithoutTrappingStay() {
        assertEquals(DinosaurCommandMode.STAY, DinosaurCommandMode.HOME.next(false));
        assertEquals(DinosaurCommandMode.HOME, DinosaurCommandMode.STAY.next(false));
        assertEquals(DinosaurCommandMode.HOME, DinosaurCommandMode.FOLLOW.next(false));
    }
}
