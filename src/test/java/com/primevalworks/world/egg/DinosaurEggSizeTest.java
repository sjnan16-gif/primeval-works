package com.primevalworks.world.egg;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DinosaurEggSizeTest {
    @Test
    void everyEggDropsOneToThreeFragmentsAndLargerEggsAverageMore() {
        double small = averageFragments(EggFragmentRules.SMALL, 14_000L);
        double big = averageFragments(EggFragmentRules.BIG, 14_000L);
        double large = averageFragments(EggFragmentRules.LARGE, 14_000L);
        assertTrue(small < big && big < large,
                "fragment averages must rise with egg size: " + small + ", " + big + ", " + large);
    }

    private static double averageFragments(EggFragmentRules size, long rolls) {
        Random random = new Random(0x5EEDL + size.ordinal());
        long total = 0L;
        boolean sawOne = false;
        boolean sawThree = false;
        for (long index = 0; index < rolls; index++) {
            int count = size.count(random.nextFloat(), random.nextFloat());
            assertTrue(count >= 1 && count <= 3, "egg fragment roll escaped its 1-3 contract");
            sawOne |= count == 1;
            sawThree |= count == 3;
            total += count;
        }
        assertTrue(sawOne && sawThree, size + " did not exercise its complete reward range");
        return total / (double)rolls;
    }
}
