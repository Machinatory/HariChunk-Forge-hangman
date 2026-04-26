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
    private static final AtomicLong fallbacks = new AtomicLong();

    private BrsGpuNoiseStats() {
    }

    static void recordNativeNoiseBatch(int samples) {
        nativeNoiseSubmissions.incrementAndGet();
        nativeNoiseSamples.addAndGet(samples);
    }

    static void recordFallback() {
        fallbacks.incrementAndGet();
    }

    public static String summary() {
        return "nativeNoise=" + nativeNoiseSubmissions.get() + "/" + nativeNoiseSamples.get()
                + " fallback=" + fallbacks.get();
    }
}
