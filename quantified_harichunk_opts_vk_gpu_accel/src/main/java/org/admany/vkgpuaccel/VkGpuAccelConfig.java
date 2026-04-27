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

    /**
     * Minimum samples per density evaluation to attempt GPU dispatch.
     * With QAPI's GpuTaskDispatcher batching (preferredBatchSize ~25-64),
     * even small evaluations get coalesced into one mcDensityFunctionsBatch
     * call, so a lower threshold is correct. 64 keeps trivially small
     * evaluations on CPU while routing anything MC actually generates to GPU.
     */
    public static final int MIN_BATCH_SIZE = Integer.getInteger(
            "harichunk.vkgpuaccel.min_batch", 64
    );

    public static final Duration TASK_TIMEOUT = Duration.ofMillis(Long.getLong(
            "harichunk.vkgpuaccel.timeout_ms", 10_000L
    ));

    /**
     * Maximum number of GPU workloads that may be in-flight concurrently.
     * QAPI's GpuTaskDispatcher accumulates tasks into batches keyed by
     * affinityKey until it reaches preferredBatchSize (dynamically ~32-64).
     * With a cap of 4, QAPI's bucket only ever saw 4 tasks and flushed on
     * the 5 ms time-out rather than filling a 32-64 item batch. Raising to
     * 64 lets QAPI fill a full batch and issue ONE mcDensityFunctionsBatch
     * Vulkan dispatch for all in-flight evaluations instead of many tiny ones.
     * QAPI's VulkanManager enforces its own VRAM guard via canAcceptTask().
     */
    public static final int MAX_IN_FLIGHT_WORKSPACES = Integer.getInteger(
            "harichunk.vkgpuaccel.max_in_flight_workspaces", 64
    );

    private VkGpuAccelConfig() {
    }
}
