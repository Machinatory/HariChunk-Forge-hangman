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

import org.admany.quantified.api.QuantifiedAPI;
import org.admany.quantified.api.compute.GpuBackendPreference;
import org.admany.quantified.api.vulkan.QuantifiedVulkan;
import org.admany.quantifiedintegration.QuantifiedIntegration;
import org.admany.quantifiedintegration.gpu.QuantifiedGpuRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VkGpuAccel {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk Vulkan GPU Acceleration By Admany");
    private static final String QAPI_MOD_ID = "harichunk";

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static volatile boolean ready;
    private static volatile String unavailableReason = "not initialized";

    private VkGpuAccel() {
    }

    public static void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        refreshAvailability();
    }

    public static boolean refreshAvailability() {
        if (!VkGpuAccelConfig.ENABLED) {
            ready = false;
            unavailableReason = "disabled";
            return false;
        }

        try {
            if (!QuantifiedIntegration.register()) {
                ready = false;
                unavailableReason = "Quantified integration registration failed";
                return false;
            }

            QuantifiedAPI.setGpuBackendPreference(QAPI_MOD_ID, GpuBackendPreference.VULKAN_REQUIRED);
            boolean available = QuantifiedGpuRuntime.isVulkanAvailable();
            if (!available) {
                QuantifiedGpuRuntime.triggerGpuProbes("harichunk-vk-accel");
            }
            ready = available;
            unavailableReason = available ? "" : "QAPI Vulkan runtime unavailable or probe pending";
            if (available) {
                LOGGER.info("QAPI Vulkan ready. HariChunk custom Vulkan acceleration by Admany owns GPU noise routing");
            }
            return available;
        } catch (Throwable throwable) {
            ready = false;
            unavailableReason = throwable.toString();
            LOGGER.debug("Vulkan accel availability check failed: {}", throwable.toString());
            return false;
        }
    }

    public static boolean isReady() {
        return ready || refreshAvailability();
    }

    public static boolean shouldOwnGpuNoise() {
        return VkGpuAccelConfig.TAKE_OVER_GPU_NOISE && isReady();
    }

    public static boolean shouldDisableLegacyOpenCl() {
        return shouldOwnGpuNoise();
    }

    public static boolean canComputeDensityBatches() {
        return canComputeEncodedDensityBatches() || canComputeApproximateDensityBatches();
    }

    public static boolean canComputeEncodedDensityBatches() {
        return shouldOwnGpuNoise();
    }

    public static boolean canComputeApproximateDensityBatches() {
        return shouldOwnGpuNoise() && VkGpuAccelConfig.ALLOW_APPROX_DENSITY;
    }

    public static String statusString() {
        return isReady() ? "ready, " + VkGpuAccelStats.summary() : "unavailable: " + unavailableReason;
    }

    public static CompletableFuture<float[]> submitTerrainFeatureBatch(float[] inputCoords) {
        Objects.requireNonNull(inputCoords, "inputCoords");
        if (inputCoords.length == 0) {
            return CompletableFuture.completedFuture(new float[0]);
        }
        if (!isReady()) {
            VkGpuAccelStats.recordFallback();
            return CompletableFuture.completedFuture(null);
        }

        int items = inputCoords.length / 3;
        VkGpuAccelStats.recordFeatureBatch(items);

        return QuantifiedAPI.<float[]>vulkan(QAPI_MOD_ID, "harichunk-vk-terrain-features")
                .cpuFallback(() -> null)
                .workload(new QuantifiedVulkan.Workload<>() {
                    @Override
                    public long estimatedVramBytes() {
                        return (long) inputCoords.length * Float.BYTES * 5L;
                    }

                    @Override
                    public int estimatedComputeUnits() {
                        return Math.max(1, inputCoords.length);
                    }

                    @Override
                    public float[] execute(QuantifiedVulkan.Context context) {
                        return context.terrainGeneration(inputCoords);
                    }
                })
                .dataSizeBytes((long) inputCoords.length * Float.BYTES * 5L)
                .parallelUnits(Math.max(1, inputCoords.length))
                .complexity(QuantifiedVulkan.Complexity.MASSIVE)
                .kind(QuantifiedVulkan.WorkloadKind.SPATIAL_ANALYSIS)
                .timeout(VkGpuAccelConfig.TASK_TIMEOUT)
                .allowMainThreadRerouting(false)
                .submit();
    }

    public static CompletableFuture<float[]> submitMcDensityFunctionBatch(float[] packedCoords,
                                                                          float[] encodedProgram,
                                                                          int instructionCount) {
        return submitMcDensityFunctionBatch(packedCoords, encodedProgram, instructionCount, new float[0], 0);
    }

    public static CompletableFuture<float[]> submitMcDensityFunctionBatch(float[] packedCoords,
                                                                          float[] encodedProgram,
                                                                          int instructionCount,
                                                                          float[] auxValues,
                                                                          int auxValueCount) {
        Objects.requireNonNull(packedCoords, "packedCoords");
        Objects.requireNonNull(encodedProgram, "encodedProgram");
        Objects.requireNonNull(auxValues, "auxValues");
        if (packedCoords.length == 0) {
            return CompletableFuture.completedFuture(new float[0]);
        }
        if (!canComputeEncodedDensityBatches()) {
            VkGpuAccelStats.recordFallback();
            return CompletableFuture.completedFuture(null);
        }

        int samples = packedCoords.length / 3;
        VkGpuAccelStats.recordDensityBatch(samples);
        if (auxValueCount < 0 || auxValues.length < auxValueCount * samples) {
            VkGpuAccelStats.recordFallback();
            return CompletableFuture.completedFuture(null);
        }
        long dataSize = (long) (packedCoords.length + encodedProgram.length + samples
                + (long) auxValueCount * samples) * Float.BYTES;

        return QuantifiedAPI.<float[]>vulkan(QAPI_MOD_ID, "harichunk-vk-mc-density")
                .cpuFallback(() -> null)
                .workload(new QuantifiedVulkan.Workload<>() {
                    @Override
                    public long estimatedVramBytes() {
                        return dataSize;
                    }

                    @Override
                    public int estimatedComputeUnits() {
                        return Math.max(1, samples);
                    }

                    @Override
                    public float[] execute(QuantifiedVulkan.Context context) {
                        return context.mcDensityFunctions(packedCoords, encodedProgram, instructionCount,
                                auxValues, auxValueCount);
                    }
                })
                .dataSizeBytes(dataSize)
                .parallelUnits(Math.max(1, samples))
                .complexity(QuantifiedVulkan.Complexity.MASSIVE)
                .kind(QuantifiedVulkan.WorkloadKind.SPATIAL_ANALYSIS)
                .timeout(VkGpuAccelConfig.TASK_TIMEOUT)
                .allowMainThreadRerouting(false)
                .submit();
    }

    public static double[] tryComputeEncodedDensityBatch(double[] xCoords,
                                                         double[] yCoords,
                                                         double[] zCoords,
                                                         int offset,
                                                         int length,
                                                         float[] encodedProgram,
                                                         int instructionCount) {
        return tryComputeEncodedDensityBatch(xCoords, yCoords, zCoords, offset, length,
                encodedProgram, instructionCount, new float[0], 0);
    }

    public static double[] tryComputeEncodedDensityBatch(double[] xCoords,
                                                         double[] yCoords,
                                                         double[] zCoords,
                                                         int offset,
                                                         int length,
                                                         float[] encodedProgram,
                                                         int instructionCount,
                                                         float[] auxValues,
                                                         int auxValueCount) {
        if (!canComputeEncodedDensityBatches() || length < VkGpuAccelConfig.MIN_BATCH_SIZE) {
            VkGpuAccelStats.recordFallback();
            return null;
        }

        float[] packed = new float[length * 3];
        for (int i = 0; i < length; i++) {
            int source = offset + i;
            int target = i * 3;
            packed[target] = (float) xCoords[source];
            packed[target + 1] = (float) yCoords[source];
            packed[target + 2] = (float) zCoords[source];
        }

        try {
            float[] gpuDensity = submitMcDensityFunctionBatch(packed, encodedProgram, instructionCount,
                            auxValues, auxValueCount)
                    .get(Math.max(1L, VkGpuAccelConfig.TASK_TIMEOUT.toMillis()), TimeUnit.MILLISECONDS);
            if (gpuDensity == null || gpuDensity.length != length) {
                VkGpuAccelStats.recordFallback();
                return null;
            }

            double[] density = new double[length];
            for (int i = 0; i < length; i++) {
                density[i] = gpuDensity[i];
            }
            return density;
        } catch (Throwable throwable) {
            VkGpuAccelStats.recordFallback();
            LOGGER.debug("VK encoded density batch failed: {}", throwable.toString());
            return null;
        }
    }

    public static double[] tryComputeEncodedDensityBatch(int[] xCoords,
                                                         int[] yCoords,
                                                         int[] zCoords,
                                                         int offset,
                                                         int length,
                                                         float[] encodedProgram,
                                                         int instructionCount) {
        return tryComputeEncodedDensityBatch(xCoords, yCoords, zCoords, offset, length,
                encodedProgram, instructionCount, new float[0], 0);
    }

    public static double[] tryComputeEncodedDensityBatch(int[] xCoords,
                                                         int[] yCoords,
                                                         int[] zCoords,
                                                         int offset,
                                                         int length,
                                                         float[] encodedProgram,
                                                         int instructionCount,
                                                         float[] auxValues,
                                                         int auxValueCount) {
        if (!canComputeEncodedDensityBatches() || length < VkGpuAccelConfig.MIN_BATCH_SIZE) {
            VkGpuAccelStats.recordFallback();
            return null;
        }

        float[] packed = new float[length * 3];
        for (int i = 0; i < length; i++) {
            int source = offset + i;
            int target = i * 3;
            packed[target] = xCoords[source];
            packed[target + 1] = yCoords[source];
            packed[target + 2] = zCoords[source];
        }

        try {
            float[] gpuDensity = submitMcDensityFunctionBatch(packed, encodedProgram, instructionCount,
                            auxValues, auxValueCount)
                    .get(Math.max(1L, VkGpuAccelConfig.TASK_TIMEOUT.toMillis()), TimeUnit.MILLISECONDS);
            if (gpuDensity == null || gpuDensity.length != length) {
                VkGpuAccelStats.recordFallback();
                return null;
            }

            double[] density = new double[length];
            for (int i = 0; i < length; i++) {
                density[i] = gpuDensity[i];
            }
            return density;
        } catch (Throwable throwable) {
            VkGpuAccelStats.recordFallback();
            LOGGER.debug("VK encoded density batch failed: {}", throwable.toString());
            return null;
        }
    }

    public static double[] tryComputeDensityBatch(double[] xCoords,
                                                  double[] yCoords,
                                                  double[] zCoords,
                                                  int offset,
                                                  int length) {
        if (!canComputeApproximateDensityBatches() || length < VkGpuAccelConfig.MIN_BATCH_SIZE) {
            VkGpuAccelStats.recordFallback();
            return null;
        }

        float[] packed = new float[length * 3];
        for (int i = 0; i < length; i++) {
            int source = offset + i;
            int target = i * 3;
            packed[target] = (float) xCoords[source];
            packed[target + 1] = (float) yCoords[source];
            packed[target + 2] = (float) zCoords[source];
        }

        try {
            float[] features = submitTerrainFeatureBatch(packed)
                    .get(Math.max(1L, VkGpuAccelConfig.TASK_TIMEOUT.toMillis()), TimeUnit.MILLISECONDS);
            if (features == null || features.length < length * 4) {
                VkGpuAccelStats.recordFallback();
                return null;
            }

            double[] density = new double[length];
            for (int i = 0; i < length; i++) {
                density[i] = (features[i * 4] * 2.0d) - 1.0d;
            }
            VkGpuAccelStats.recordDensityBatch(length);
            return density;
        } catch (Throwable throwable) {
            VkGpuAccelStats.recordFallback();
            LOGGER.debug("VK density batch failed: {}", throwable.toString());
            return null;
        }
    }
}
