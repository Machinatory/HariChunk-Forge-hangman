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

import java.time.Duration;

public final class VkGpuAccelConfig {

    public static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("harichunk.vkgpuaccel.enabled", "true")
    );

    public static final boolean TAKE_OVER_GPU_NOISE = Boolean.parseBoolean(
            System.getProperty("harichunk.vkgpuaccel.take_over_gpu_noise", "true")
    );

    /**
     * Keep false by default. QAPI's current public terrainGeneration kernel emits
     * generic terrain features, not exact Minecraft DensityFunction noise values.
     */
    public static final boolean ALLOW_APPROX_DENSITY = Boolean.parseBoolean(
            System.getProperty("harichunk.vkgpuaccel.allow_approx_density", "false")
    );

    public static final int MIN_BATCH_SIZE = Integer.getInteger(
            "harichunk.vkgpuaccel.min_batch", 256
    );

    public static final Duration TASK_TIMEOUT = Duration.ofMillis(Long.getLong(
            "harichunk.vkgpuaccel.timeout_ms", 10_000L
    ));

    private VkGpuAccelConfig() {
    }
}
