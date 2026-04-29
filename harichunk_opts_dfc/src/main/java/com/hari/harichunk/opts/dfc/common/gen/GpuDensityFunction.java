package com.hari.harichunk.opts.dfc.common.gen;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Routes DFC noise subtree batch evaluation through GPU acceleration.
 *
 * When DFC compiles density functions, noise nodes call InvocationShim
 * per-sample. For large batches (fillArray), this class provides an
 * accelerated path that collects noise coordinates and dispatches them
 * to GPU in a single batch.
 *
 * This bridges the DFC bytecode compilation layer with the GPU compute
 * layer from harichunk_quantified.
 */
public final class GpuDensityFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk/DFC-GPU");

        private static final AtomicLong encodedAttempts = new AtomicLong();
        private static final AtomicLong encodedSuccesses = new AtomicLong();
        private static final AtomicLong encodedBypasses = new AtomicLong();
        private static final AtomicLong encodedFailures = new AtomicLong();
        private static final AtomicLong approximateAttempts = new AtomicLong();
        private static final AtomicLong approximateSuccesses = new AtomicLong();
        private static final AtomicLong approximateBypasses = new AtomicLong();
        private static final AtomicLong approximateFailures = new AtomicLong();

    private static volatile boolean gpuAvailable = false;
    private static volatile boolean initialized = false;

    private GpuDensityFunction() {}

    /**
     * Initialize GPU density function acceleration.
     * Probes GPU availability from Quantified backend.
     */
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        gpuAvailable = queryBackendShouldUseGpu(1);

        LOGGER.info("DFC-GPU density function acceleration [available={}, adaptive=true]",
            gpuAvailable);
    }

    /**
     * Check if GPU acceleration should be used for a batch of given size.
     */
    public static boolean shouldUseGpuBatch(int batchSize) {
        if (batchSize <= 0) {
            return false;
        }
        if (gpuAvailable) return true;
        boolean available = queryBackendShouldUseGpu(batchSize);
        gpuAvailable = available;
        return available;
    }

    public static String debugString() {
        return "available=" + gpuAvailable
                + " encoded=" + encodedAttempts.get() + "/" + encodedSuccesses.get()
                + " bypass=" + encodedBypasses.get()
                + " fail=" + encodedFailures.get()
                + " approx=" + approximateAttempts.get() + "/" + approximateSuccesses.get()
                + " bypass=" + approximateBypasses.get()
                + " fail=" + approximateFailures.get();
    }

    /**
     * Batch-evaluate noise holder samples, routing through GPU if beneficial.
     *
     * @param noiseHolder the Minecraft noise holder
     * @param xCoords scaled x coordinates (already multiplied by xzScale + shift)
     * @param yCoords scaled y coordinates (already multiplied by yScale + shift)
     * @param zCoords scaled z coordinates (already multiplied by xzScale + shift)
     * @param result output array to fill
     * @param offset start index in arrays
     * @param length number of samples to compute
     */
    public static void batchNoiseSample(
            DensityFunction.NoiseHolder noiseHolder,
            double[] xCoords, double[] yCoords, double[] zCoords,
            double[] result, int offset, int length) {

        if (length <= 0) return;

        // Try GPU acceleration for large batches
        if (shouldUseApproximateNoiseBatch(length)) {
            approximateAttempts.incrementAndGet();
            try {
                if (tryGpuBatchNoise(noiseHolder, xCoords, yCoords, zCoords, result, offset, length)) {
                    approximateSuccesses.incrementAndGet();
                    return;
                }
                approximateFailures.incrementAndGet();
            } catch (Exception e) {
                approximateFailures.incrementAndGet();
                LOGGER.debug("GPU batch noise failed, falling back to per-sample: {}", e.getMessage());
            }
        } else {
            approximateBypasses.incrementAndGet();
        }

        // Fallback: per-sample evaluation via InvocationShim
        for (int i = 0; i < length; i++) {
            result[offset + i] = noiseHolder.getValue(
                    xCoords[offset + i], yCoords[offset + i], zCoords[offset + i]);
        }
    }

    public static boolean tryEvaluateEncodedProgram(float[] encodedProgram,
                                                    int instructionCount,
                                                    double[] result,
                                                    int[] xCoords,
                                                    int[] yCoords,
                                                    int[] zCoords) {
        return tryEvaluateEncodedProgram(encodedProgram, instructionCount, result, xCoords, yCoords, zCoords, null, 0);
    }

    public static boolean tryEvaluateEncodedProgram(float[] encodedProgram,
                                                    int instructionCount,
                                                    double[] result,
                                                    int[] xCoords,
                                                    int[] yCoords,
                                                    int[] zCoords,
                                                    float[] auxValues,
                                                    int auxValueCount) {
        if (result.length <= 0 || !shouldUseGpuBatch(result.length)) {
            encodedBypasses.incrementAndGet();
            return false;
        }
        encodedAttempts.incrementAndGet();
        try {
            Class<?> accelClass = Class.forName("org.admany.vkgpuaccel.VkGpuAccel");
            Object computed;
            try {
                computed = accelClass.getMethod(
                                "tryComputeEncodedDensityBatch",
                                int[].class, int[].class, int[].class,
                                int.class, int.class,
                                float[].class, int.class,
                                float[].class, int.class)
                        .invoke(null, xCoords, yCoords, zCoords, 0, result.length,
                                encodedProgram, instructionCount,
                                auxValues == null ? new float[0] : auxValues, auxValueCount);
            } catch (NoSuchMethodException ignored) {
                if (auxValueCount > 0) {
                    return false;
                }
                computed = accelClass.getMethod(
                            "tryComputeEncodedDensityBatch",
                            int[].class, int[].class, int[].class,
                            int.class, int.class,
                            float[].class, int.class)
                        .invoke(null, xCoords, yCoords, zCoords, 0, result.length, encodedProgram, instructionCount);
            }
            if (!(computed instanceof double[] values) || values.length != result.length) {
                encodedFailures.incrementAndGet();
                return false;
            }
            System.arraycopy(values, 0, result, 0, result.length);
            encodedSuccesses.incrementAndGet();
            return true;
        } catch (Exception e) {
            encodedFailures.incrementAndGet();
            LOGGER.debug("Encoded Vulkan density program unavailable: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Attempt GPU batch noise computation via Quantified's terrain task.
     */
    private static boolean tryGpuBatchNoise(
            DensityFunction.NoiseHolder noiseHolder,
            double[] xCoords, double[] yCoords, double[] zCoords,
            double[] result, int offset, int length) throws Exception {

        // Use Quantified's TerrainHeightMapTask for GPU-accelerated terrain computation
        Class<?> thermalExecClass = Class.forName(
                "com.hari.harichunk.opts.gpu_noise.common.ThermalAwareExecutor");

        @SuppressWarnings("unchecked")
        java.util.concurrent.CompletableFuture<double[]> future =
                (java.util.concurrent.CompletableFuture<double[]>)
                        thermalExecClass.getMethod("executeAsync",
                                java.util.function.Supplier.class,
                                java.util.function.Supplier.class,
                                int.class)
                        .invoke(null,

                                // GPU work: attempt terrain generation via Quantified
                                (java.util.function.Supplier<double[]>) () -> {
                                    double[] generated = quantifiedTerrainGenerate(
                                            noiseHolder, xCoords, yCoords, zCoords, offset, length);
                                    if (generated == null || generated.length != length) {
                                        return null;
                                    }
                                    double[] gpuResult = new double[length];
                                    System.arraycopy(generated, 0, gpuResult, 0, length);
                                    return gpuResult;
                                },

                                // CPU fallback
                                (java.util.function.Supplier<double[]>) () -> {
                                    double[] cpuResult = new double[length];
                                    for (int i = 0; i < length; i++) {
                                        cpuResult[i] = noiseHolder.getValue(
                                                xCoords[offset + i],
                                                yCoords[offset + i],
                                                zCoords[offset + i]);
                                    }
                                    return cpuResult;
                                },

                                length
                        );

        double[] gpuResult = future.get();
        if (gpuResult != null && gpuResult.length == length) {
            System.arraycopy(gpuResult, 0, result, offset, length);
            return true;
        }
        return false;
    }

    /**
     * Invoke Quantified-API terrain generation for noise computation.
     * Uses reflection to avoid hard dependency on harichunk_quantified/harichunk_opts_gpu_noise.
     */
    private static double[] quantifiedTerrainGenerate(
            DensityFunction.NoiseHolder noiseHolder,
            double[] xCoords, double[] yCoords, double[] zCoords,
            int offset, int length) {

        // Attempt to use HariChunk's custom Vulkan accel owner. The default path
        // stays exact: if the VK module cannot provide a safe result, return null
        // and let the caller use the CPU fallback.
        try {
            Class<?> accelClass = Class.forName("org.admany.vkgpuaccel.VkGpuAccel");
            Object result = accelClass.getMethod(
                            "tryComputeDensityBatch",
                            double[].class, double[].class, double[].class, int.class, int.class)
                    .invoke(null, xCoords, yCoords, zCoords, offset, length);
            return result instanceof double[] values ? values : null;
        } catch (Exception e) {
            LOGGER.debug("Vulkan terrain generation not available: {}", e.getMessage());
        }

        // If Vulkan terrain API is not directly usable, return null to trigger fallback.
        return null;
    }

    private static boolean shouldUseApproximateNoiseBatch(int batchSize) {
        if (!shouldUseGpuBatch(batchSize)) {
            return false;
        }
        try {
            Class<?> accelClass = Class.forName("org.admany.vkgpuaccel.VkGpuAccel");
            Object result = accelClass.getMethod("canComputeApproximateDensityBatches").invoke(null);
            return result instanceof Boolean value && value;
        } catch (Exception ignored) {
            return true;
        }
    }

    /**
     * Get GPU availability status.
     */
    public static boolean isGpuAvailable() {
        return gpuAvailable;
    }

    private static boolean queryBackendShouldUseGpu(int batchSize) {
        try {
            Class<?> accelClass = Class.forName("org.admany.vkgpuaccel.VkGpuAccel");
            Object ready = accelClass.getMethod("isReady").invoke(null);
            if (ready instanceof Boolean value && value) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {
            // Fall through to legacy OpenCL/C2ME-style backend checks.
        } catch (Exception e) {
            LOGGER.debug("DFC-GPU: Vulkan GPU probe failed: {}", e.getMessage());
        }

        try {
            Class<?> backendClass = Class.forName(
                    "com.hari.harichunk.opts.gpu_noise.common.GpuNoiseBackend");
            java.lang.reflect.Method shouldUseGpu = backendClass.getMethod(
                    "shouldUseGpu", int.class);
            return (boolean) shouldUseGpu.invoke(null, batchSize);
        } catch (ClassNotFoundException e) {
            LOGGER.info("DFC-GPU: harichunk_opts_gpu_noise module not present, GPU acceleration disabled");
            return false;
        } catch (Exception e) {
            LOGGER.debug("DFC-GPU: GPU probe failed: {}", e.getMessage());
            return false;
        }
    }
}
