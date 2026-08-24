package com.primevalworks.world.work;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

public final class PriorityRouting {
    private PriorityRouting() {
    }

    public static <T> List<T> order(
            List<T> candidates,
            ToIntFunction<T> priority,
            ToDoubleFunction<T> distance,
            boolean nearestWithinPriority
    ) {
        Comparator<T> order = Comparator.comparingInt(priority).reversed();
        if (nearestWithinPriority) {
            order = order.thenComparingDouble(distance);
        }
        return candidates.stream().sorted(order).toList();
    }
}
