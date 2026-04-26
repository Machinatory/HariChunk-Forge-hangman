package com.hari.harichunk.opts.gpu_noise.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Wraps GPU noise batch submissions with thermal awareness.
 * Automatically falls back to CPU when GPU temperature exceeds safe threshold.
 */
public final class ThermalAwareExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk GPU Noise/Thermal");

    private ThermalAwareExecutor() {}

    /**
     * Execute a noise batch, choosing GPU or CPU based on thermal state.
     *
     * @param gpuWork    the GPU-accelerated computation
     * @param cpuFallback the CPU fallback computation
     * @param batchSize  the number of noise values in this batch
     * @return the computed noise values
     */
    public static double[] execute(Supplier<double[]> gpuWork, Supplier<double[]> cpuFallback, int batchSize) {
        if (!GpuNoiseBackend.shouldUseGpu(batchSize)) {
            if (cpuFallback != null) {
                return cpuFallback.get();
            }
            throw new IllegalStateException("GPU disabled and no CPU fallback was supplied");
        }

        try {
            double[] result = gpuWork.get();
            if (result != null) {
                return result;
            }
            if (cpuFallback != null) {
                return cpuFallback.get();
            }
            throw new IllegalStateException("GPU noise computation returned no result");
        } catch (Exception e) {
            LOGGER.warn("GPU noise computation failed, falling back to CPU: {}", e.getMessage());
            if (cpuFallback != null) {
                return cpuFallback.get();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Async variant - executes on GPU if available and thermally safe,
     * otherwise falls back to CPU on a worker thread.
     */
    public static CompletableFuture<double[]> executeAsync(
            Supplier<double[]> gpuWork,
            Supplier<double[]> cpuFallback,
            int batchSize) {
        if (!GpuNoiseBackend.shouldUseGpu(batchSize)) {
            if (cpuFallback == null) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("GPU disabled and no CPU fallback was supplied"));
            }
            return CompletableFuture.supplyAsync(cpuFallback);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                double[] result = gpuWork.get();
                if (result != null) {
                    return result;
                }
                if (cpuFallback != null) {
                    return cpuFallback.get();
                }
                throw new IllegalStateException("GPU noise computation returned no result");
            } catch (Exception e) {
                LOGGER.warn("GPU noise computation failed, falling back to CPU: {}", e.getMessage());
                if (cpuFallback != null) {
                    return cpuFallback.get();
                }
                throw new RuntimeException(e);
            }
        });
    }
}
