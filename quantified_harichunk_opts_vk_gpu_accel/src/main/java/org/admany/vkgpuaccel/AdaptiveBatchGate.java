package org.admany.vkgpuaccel;

import java.util.concurrent.atomic.AtomicInteger;

public final class AdaptiveBatchGate {

    private final int maxThreshold;
    private final AtomicInteger currentThreshold = new AtomicInteger(1);

    public AdaptiveBatchGate(int maxThreshold) {
        this.maxThreshold = Math.max(1, maxThreshold);
    }

    public boolean shouldAttempt(int batchSize) {
        return batchSize >= currentThreshold.get();
    }

    public void recordSuccess() {
        currentThreshold.getAndUpdate(value -> value <= 1 ? 1 : Math.max(1, value / 2));
    }

    public void recordBackpressure() {
        currentThreshold.getAndUpdate(value -> {
            if (value >= maxThreshold) {
                return maxThreshold;
            }
            return Math.min(maxThreshold, Math.max(value + 1, value * 2));
        });
    }

    public void recordFailure() {
        currentThreshold.getAndUpdate(value -> {
            if (value >= maxThreshold) {
                return maxThreshold;
            }
            return Math.min(maxThreshold, value + 1);
        });
    }

    public int currentThreshold() {
        return currentThreshold.get();
    }

    public int maxThreshold() {
        return maxThreshold;
    }

    public String summary() {
        return currentThreshold() + "/" + maxThreshold;
    }
}