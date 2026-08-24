package com.primevalworks.client.render.entity;

final class MountAttachmentFilter {
    private static final double AQUATIC_RESPONSE = 42.0D;

    private MountAttachmentFilter() {
    }

    static double alpha(boolean aquatic, double deltaSeconds) {
        if (!aquatic) return 1.0D;
        double frameTime = Math.max(0.001D, Math.min(0.05D, deltaSeconds));
        return 1.0D - Math.exp(-AQUATIC_RESPONSE * frameTime);
    }
}
