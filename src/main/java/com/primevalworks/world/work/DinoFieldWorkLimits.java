package com.primevalworks.world.work;

public final class DinoFieldWorkLimits {
    public static final int MAX_CONNECTED_BLOCKS = 64;
    public static final int MAX_AREA_BLOCKS = 512;
    public static final int MAX_AREA_SPAN = 16;

    private DinoFieldWorkLimits() {}

    public static boolean areaWithinLimits(int firstX, int firstY, int firstZ,
                                           int secondX, int secondY, int secondZ) {
        long x = Math.abs((long)firstX - secondX) + 1L;
        long y = Math.abs((long)firstY - secondY) + 1L;
        long z = Math.abs((long)firstZ - secondZ) + 1L;
        return x <= MAX_AREA_SPAN && y <= MAX_AREA_SPAN && z <= MAX_AREA_SPAN
                && x * y * z <= MAX_AREA_BLOCKS;
    }
}
