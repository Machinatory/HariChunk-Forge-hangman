/*
 * HariChunk Quantified Vulkan GPU Acceleration by Admany / BlackRift Studios.
 * Licensed strictly under BRSSLA V1.5.
 * This is original BlackRift Studios code and is not related to C2ME in any way.
 *
 * This source may be viewed and contributed to through approved project channels.
 * Modification, copying, redistribution, reuse, or derivative use outside those
 * approved contribution flows is strictly not allowed. C2ME may not copy, modify,
 * reuse, redistribute, or derive from this code.
 */
package org.admany.vkgpuaccel;

import java.util.concurrent.atomic.AtomicLong;

public final class VkGpuAccelStats {

    private static final AtomicLong featureSubmissions = new AtomicLong();
    private static final AtomicLong featureItems = new AtomicLong();
    private static final AtomicLong densitySubmissions = new AtomicLong();
    private static final AtomicLong densityItems = new AtomicLong();
    private static final AtomicLong attempts = new AtomicLong();
    private static final AtomicLong successes = new AtomicLong();
    private static final AtomicLong adaptiveSkips = new AtomicLong();
    private static final AtomicLong unavailable = new AtomicLong();
    private static final AtomicLong busy = new AtomicLong();
    private static final AtomicLong invalidInputs = new AtomicLong();
    private static final AtomicLong nullResults = new AtomicLong();
    private static final AtomicLong failures = new AtomicLong();
    private static final AtomicLong compiledShaders = new AtomicLong();

    private VkGpuAccelStats() {
    }

    static void recordFeatureBatch(int items) {
        featureSubmissions.incrementAndGet();
        featureItems.addAndGet(items);
    }

    static void recordDensityBatch(int items) {
        densitySubmissions.incrementAndGet();
        densityItems.addAndGet(items);
    }

    static void recordAttempt() {
        attempts.incrementAndGet();
    }

    static void recordSuccess() {
        successes.incrementAndGet();
    }

    static void recordAdaptiveSkip() {
        adaptiveSkips.incrementAndGet();
    }

    static void recordUnavailable() {
        unavailable.incrementAndGet();
    }

    static void recordBusy() {
        busy.incrementAndGet();
    }

    static void recordInvalidInput() {
        invalidInputs.incrementAndGet();
    }

    static void recordNullResult() {
        nullResults.incrementAndGet();
    }

    static void recordFailure() {
        failures.incrementAndGet();
    }

    static void recordCompiledShader() {
        compiledShaders.incrementAndGet();
    }

    public static String summary() {
        return "submitted=" + featureSubmissions.get() + "/" + featureItems.get()
            + "+" + densitySubmissions.get() + "/" + densityItems.get()
            + " attempts=" + attempts.get()
            + " success=" + successes.get()
                + " compiled=" + compiledShaders.get()
            + " skip=" + adaptiveSkips.get()
            + " unavailable=" + unavailable.get()
            + " busy=" + busy.get()
            + " invalid=" + invalidInputs.get()
            + " null=" + nullResults.get()
            + " failed=" + failures.get();
    }
}
