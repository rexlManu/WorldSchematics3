package optic_fusion1.worldschematics.util;

import com.google.common.util.concurrent.AtomicDouble;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import worldschematics.util.DebugLogger;

public final class MapUtils {

    private MapUtils() {
    }

    public static <T> T getFromWeightedMap(Map<T, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            DebugLogger.log("Weighted table is empty!", DebugLogger.DebugType.LOOTTABLE);
            return null;
        }
        double chance = ThreadLocalRandom.current().nextDouble() * weights.values().stream().reduce(0D, Double::sum);
        AtomicDouble needle = new AtomicDouble();
        return weights.entrySet().stream().filter((ent) -> {
            return needle.addAndGet(ent.getValue()) >= chance;
        }).findFirst().map(Map.Entry::getKey).orElse(null);
    }

}
