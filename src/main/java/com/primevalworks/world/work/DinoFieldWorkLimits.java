package com.primevalworks.world.work;

import com.primevalworks.world.entity.DinosaurProgression;

public final class DinoFieldWorkLimits {
    public static final int MAX_CONNECTED_BLOCKS = 256;
    public static final int MAX_AREA_BLOCKS = 8192;
    public static final int MAX_AREA_SPAN = 32;
    private static final int MIN_AREA_BLOCKS = 512;
    private static final int MIN_AREA_SPAN = 12;

    private DinoFieldWorkLimits() {}

    public static boolean areaWithinLimits(int firstX, int firstY, int firstZ,
                                           int secondX, int secondY, int secondZ) {
        long x = Math.abs((long)firstX - secondX) + 1L;
        long y = Math.abs((long)firstY - secondY) + 1L;
        long z = Math.abs((long)firstZ - secondZ) + 1L;
        return x <= MAX_AREA_SPAN && y <= MAX_AREA_SPAN && z <= MAX_AREA_SPAN
                && x * y * z <= MAX_AREA_BLOCKS;
    }

    public static boolean areaWithinLimits(int firstX, int firstY, int firstZ,
                                           int secondX, int secondY, int secondZ, int dinosaurLevel) {
        long x = Math.abs((long)firstX - secondX) + 1L;
        long y = Math.abs((long)firstY - secondY) + 1L;
        long z = Math.abs((long)firstZ - secondZ) + 1L;
        int span = maximumAreaSpan(dinosaurLevel);
        return x <= span && y <= span && z <= span
                && x * y * z <= maximumAreaBlocks(dinosaurLevel);
    }

    public static int maximumAreaBlocks(int dinosaurLevel) {
        int level = Math.max(1, Math.min(DinosaurProgression.MAX_LEVEL, dinosaurLevel));
        return MIN_AREA_BLOCKS + Math.round((MAX_AREA_BLOCKS - MIN_AREA_BLOCKS) * (level - 1)
                / (float)(DinosaurProgression.MAX_LEVEL - 1));
    }

    public static int maximumAreaSpan(int dinosaurLevel) {
        int level = Math.max(1, Math.min(DinosaurProgression.MAX_LEVEL, dinosaurLevel));
        return MIN_AREA_SPAN + Math.round((MAX_AREA_SPAN - MIN_AREA_SPAN) * (level - 1)
                / (float)(DinosaurProgression.MAX_LEVEL - 1));
    }

    public static int requiredLevel(int firstX, int firstY, int firstZ,
                                    int secondX, int secondY, int secondZ) {
        if (!areaWithinLimits(firstX, firstY, firstZ, secondX, secondY, secondZ)) {
            return DinosaurProgression.MAX_LEVEL + 1;
        }
        for (int level = 1; level <= DinosaurProgression.MAX_LEVEL; level++) {
            if (areaWithinLimits(firstX, firstY, firstZ, secondX, secondY, secondZ, level)) return level;
        }
        return DinosaurProgression.MAX_LEVEL + 1;
    }
}
