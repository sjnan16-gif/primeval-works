package com.primevalworks.world.work;

import java.util.Collection;

public final class WorkTargetRules {
    private WorkTargetRules() {
    }

    public static boolean acceptsNonEmptyTargetCount(int count, int maximum) {
        return count > 0 && count <= maximum;
    }

    public static boolean routesDoNotOverlap(Collection<?> sources, Collection<?> destinations) {
        return sources.stream().noneMatch(destinations::contains);
    }
}
