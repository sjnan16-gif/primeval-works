package com.primevalworks.world.work;

public final class WorkTargetRules {
    private WorkTargetRules() {
    }

    public static boolean acceptsNonEmptyTargetCount(int count, int maximum) {
        return count > 0 && count <= maximum;
    }
}
