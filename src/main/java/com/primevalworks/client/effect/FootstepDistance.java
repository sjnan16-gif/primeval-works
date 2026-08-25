package com.primevalworks.client.effect;

public final class FootstepDistance {
    private FootstepDistance() {
    }

    public static double toBox(
            double x,
            double y,
            double z,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        double nearestX = Math.max(minX, Math.min(maxX, x));
        double nearestY = Math.max(minY, Math.min(maxY, y));
        double nearestZ = Math.max(minZ, Math.min(maxZ, z));
        double dx = x - nearestX;
        double dy = y - nearestY;
        double dz = z - nearestZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
