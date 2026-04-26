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

import java.time.Duration;

public final class BrsGpuNoiseConfig {

    public static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("harichunk.brs_gpu_noise.enabled", "true")
    );

    /**
     * This is intentionally separate from the generic VK density path. It uses a
     * custom deterministic GPU value-noise route, so it is fast and stable, but
     * it is not byte-for-byte vanilla MC noise.
     */
    public static final boolean NATIVE_NOISE = Boolean.parseBoolean(
            System.getProperty("harichunk.brs_gpu_noise.native_noise", "true")
    );

    public static final int MIN_BATCH_SIZE = Integer.getInteger(
            "harichunk.brs_gpu_noise.min_batch", 192
    );

    public static final Duration TASK_TIMEOUT = Duration.ofMillis(Long.getLong(
            "harichunk.brs_gpu_noise.timeout_ms", 10_000L
    ));

    private BrsGpuNoiseConfig() {
    }
}
