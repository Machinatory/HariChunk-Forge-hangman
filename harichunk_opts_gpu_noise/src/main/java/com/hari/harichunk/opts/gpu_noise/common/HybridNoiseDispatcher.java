package com.hari.harichunk.opts.gpu_noise.common;

import com.hari.harichunk.opts.natives_math.common.NativeBindings;
import com.hari.harichunk.opts.natives_math.common.NativeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Unified noise dispatcher that routes computation to the optimal backend:
 * <ul>
 *   <li>Small batches (below GPU threshold) → Native SIMD CPU (AVX2/AVX512) if available</li>
 *   <li>Large batches (above GPU threshold) → GPU (Vulkan > OpenCL) if available</li>
 *   <li>Fallback → plain Java computation</li>
 * </ul>
 *
 * Bridges harichunk_natives_opts (SIMD) and harichunk_quantified (GPU) into a single dispatch point.
 */
public final class HybridNoiseDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk/HybridNoise");

    // Threshold: batches below this prefer SIMD CPU (low latency, no GPU dispatch overhead)
    public static final int SIMD_PREFERRED_BATCH_THRESHOLD = Integer.getInteger(
            "harichunk.hybrid.simd_threshold", 256
    );

    private static volatile boolean nativeSimdAvailable = false;
    private static volatile boolean gpuAvailable = false;
    private static volatile boolean initialized = false;

    private HybridNoiseDispatcher() {}

    /**
     * Initialize the hybrid dispatcher, detecting available backends.
     */
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        // Detect native SIMD availability
        try {
            nativeSimdAvailable = NativeLoader.available;
            if (nativeSimdAvailable) {
                LOGGER.info("HybridNoise: Native SIMD available (ISA: {})", NativeLoader.currentMachineTarget);
            } else {
                LOGGER.info("HybridNoise: Native SIMD not available");
            }
        } catch (NoClassDefFoundError e) {
            LOGGER.info("HybridNoise: harichunk_natives_opts module not present, SIMD unavailable");
            nativeSimdAvailable = false;
        }

        // GPU availability is checked dynamically via GpuNoiseBackend
        try {
            gpuAvailable = GpuNoiseBackend.shouldUseGpu(1);
            LOGGER.info("HybridNoise: GPU backend {}",
                    gpuAvailable ? "available (" + GpuNoiseBackend.getActiveBackend() + ")" : "not available");
        } catch (Exception e) {
            LOGGER.info("HybridNoise: GPU backend check failed: {}", e.getMessage());
            gpuAvailable = false;
        }

        LOGGER.info("HybridNoise dispatcher ready [SIMD={}, GPU={}, simdThreshold={}]",
                nativeSimdAvailable, gpuAvailable, SIMD_PREFERRED_BATCH_THRESHOLD);
    }

    /**
     * Dispatch a single noise sample to the best available backend.
     * Priority: Native SIMD (if available) → GPU (if available) → Java fallback.
     *
     * @param javaFallback plain Java noise computation
     * @param nativePtr    native data pointer (from NativeStructs), or 0 if N/A
     * @param nativeFn     native computation function (dataPtr, x, y, z) → double
     * @param x            x coordinate
     * @param y            y coordinate
     * @param z            z coordinate
     * @return computed noise value
     */
    public static double dispatchSingle(
            Supplier<Double> javaFallback,
            long nativePtr,
            NativeSingleFn nativeFn,
            double x, double y, double z) {

        // Single sample: prefer native SIMD (lowest latency, no GPU dispatch overhead)
        if (nativeSimdAvailable && nativePtr != 0) {
            try {
                return nativeFn.compute(nativePtr, x, y, z);
            } catch (Exception e) {
                LOGGER.debug("Native SIMD single failed, falling back: {}", e.getMessage());
            }
        }

        return javaFallback.get();
    }

    /**
     * Dispatch a batch of noise computations to the best available backend.
     * Small batches → SIMD CPU; large batches → GPU.
     *
     * @param javaFallback plain Java batch computation
     * @param gpuWork      GPU-accelerated batch computation
     * @param nativePtr    native data pointer, or 0
     * @param nativeBatchFn native batch function (dataPtr, res[], x[], y[], z[], len)
     * @param x            x coordinates array
     * @param y            y coordinates array
     * @param z            z coordinates array
     * @param length       number of samples
     * @return computed noise values
     */
    public static double[] dispatchBatch(
            Supplier<double[]> javaFallback,
            Supplier<double[]> gpuWork,
            long nativePtr,
            NativeBatchFn nativeBatchFn,
            double[] x, double[] y, double[] z,
            int length) {

        // GPU-ready paths may batch dynamically downstream, so do not hard-block
        // small work here before the backend gets a chance to coalesce it.
        if (GpuNoiseBackend.shouldUseGpu(length)) {
            try {
                return ThermalAwareExecutor.execute(gpuWork, null, length);
            } catch (Exception e) {
                LOGGER.debug("GPU batch failed, trying SIMD: {}", e.getMessage());
            }
        }

        // Small batch or GPU unavailable → Native SIMD
        if (nativeSimdAvailable && nativePtr != 0) {
            try {
                double[] res = new double[length];
                nativeBatchFn.compute(nativePtr, res, x, y, z, length);
                return res;
            } catch (Exception e) {
                LOGGER.debug("Native SIMD batch failed, falling back to Java: {}", e.getMessage());
            }
        }

        // Final fallback: Java
        return javaFallback.get();
    }

    /**
     * Async batch dispatch - returns CompletableFuture.
     */
    public static CompletableFuture<double[]> dispatchBatchAsync(
            Supplier<double[]> javaFallback,
            Supplier<double[]> gpuWork,
            long nativePtr,
            NativeBatchFn nativeBatchFn,
            double[] x, double[] y, double[] z,
            int length) {

        if (GpuNoiseBackend.shouldUseGpu(length)) {
            return ThermalAwareExecutor.executeAsync(gpuWork, null, length)
                    .exceptionally(ex -> {
                        LOGGER.debug("Async GPU batch failed: {}", ex.getMessage());
                        return null;
                    })
                    .thenCompose(result -> {
                        if (result != null) return CompletableFuture.completedFuture(result);
                        // Try SIMD fallback
                        if (nativeSimdAvailable && nativePtr != 0) {
                            return CompletableFuture.supplyAsync(() -> {
                                double[] res = new double[length];
                                try {
                                    nativeBatchFn.compute(nativePtr, res, x, y, z, length);
                                    return res;
                                } catch (Exception e) {
                                    return javaFallback.get();
                                }
                            });
                        }
                        return CompletableFuture.supplyAsync(javaFallback);
                    });
        }

        // Small batch or no GPU → SIMD or Java (synchronous is fine for small batches)
        if (nativeSimdAvailable && nativePtr != 0) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    double[] res = new double[length];
                    nativeBatchFn.compute(nativePtr, res, x, y, z, length);
                    return res;
                } catch (Exception e) {
                    return javaFallback.get();
                }
            });
        }

        return CompletableFuture.supplyAsync(javaFallback);
    }

    /**
     * Check if native SIMD is available.
     */
    public static boolean isSimdAvailable() {
        return nativeSimdAvailable;
    }

    /**
     * Get the optimal backend name for logging/status.
     */
    public static String getOptimalBackend(int batchSize) {
        if (gpuAvailable && GpuNoiseBackend.shouldUseGpu(batchSize)) {
            return "GPU(" + GpuNoiseBackend.getActiveBackend() + ")";
        }
        if (nativeSimdAvailable) {
            return "SIMD(" + NativeLoader.currentMachineTarget + ")";
        }
        return "Java";
    }

    /**
     * Functional interface for native single-sample computation.
     */
    @FunctionalInterface
    public interface NativeSingleFn {
        double compute(long dataPtr, double x, double y, double z);
    }

    /**
     * Functional interface for native batch computation.
     */
    @FunctionalInterface
    public interface NativeBatchFn {
        void compute(long dataPtr, double[] res, double[] x, double[] y, double[] z, int length);
    }
}
