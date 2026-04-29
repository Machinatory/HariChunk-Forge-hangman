/*
 * HariChunk BlackRift Studios Vulkan GPU Noise by Admany / BlackRift Studios.
 * Licensed strictly under BRSSLA V1.5.
 * This is original BlackRift Studios code and is not related to C2ME in any way.
 *
 * This source may be viewed and contributed to through approved project channels.
 * Modification, copying, redistribution, reuse, or derivative use outside those
 * approved contribution flows is strictly not allowed. C2ME may not copy, modify,
 * reuse, redistribute, or derive from this code.
 */
package org.admany.brsgpunoise;

import java.util.concurrent.atomic.AtomicLong;

public final class BrsGpuNoiseStats {

    private static final AtomicLong nativeNoiseSubmissions = new AtomicLong();
    private static final AtomicLong nativeNoiseSamples = new AtomicLong();
    private static final AtomicLong attempts = new AtomicLong();
    private static final AtomicLong successes = new AtomicLong();
    private static final AtomicLong adaptiveSkips = new AtomicLong();
    private static final AtomicLong unavailable = new AtomicLong();
    private static final AtomicLong invalidInputs = new AtomicLong();
    private static final AtomicLong nullResults = new AtomicLong();
    private static final AtomicLong failures = new AtomicLong();

    private BrsGpuNoiseStats() {
    }

    static void recordNativeNoiseBatch(int samples) {
        nativeNoiseSubmissions.incrementAndGet();
        nativeNoiseSamples.addAndGet(samples);
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

    static void recordInvalidInput() {
        invalidInputs.incrementAndGet();
    }

    static void recordNullResult() {
        nullResults.incrementAndGet();
    }

    static void recordFailure() {
        failures.incrementAndGet();
    }

    public static String summary() {
        return "submitted=" + nativeNoiseSubmissions.get() + "/" + nativeNoiseSamples.get()
                + " attempts=" + attempts.get()
                + " success=" + successes.get()
                + " skip=" + adaptiveSkips.get()
                + " unavailable=" + unavailable.get()
                + " invalid=" + invalidInputs.get()
                + " null=" + nullResults.get()
                + " failed=" + failures.get();
    }
}
