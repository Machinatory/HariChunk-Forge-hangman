package com.hari.harichunk.opts.gpu_noise;

import com.hari.harichunk.opts.gpu_noise.common.Config;
import com.hari.harichunk.opts.gpu_noise.common.GpuNoiseBackend;
import com.hari.harichunk.opts.gpu_noise.common.HybridNoiseDispatcher;
import com.hari.harichunk.opts.gpu_noise.common.OpenCLManager;
import com.hari.harichunk.opts.gpu_noise.common.TerrainCache;
import org.admany.quantifiedintegration.gpu.QuantifiedGpuRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk GPU Noise");

    public static boolean enabled = Config.ENABLE_GPU_NOISE;

    public static void init() {
        if (!enabled) {
            LOGGER.info("GPU Noise module disabled by config");
            return;
        }

        if (customVulkanAccelOwnsGpuNoise()) {
            LOGGER.info("Custom Quantified Vulkan accel is active; skipping legacy OpenCL GPU noise init");
        } else {
            // Initialize HariChunk's own OpenCL (legacy backend)
            try {
                OpenCLManager.initialize();
                LOGGER.info("Legacy OpenCL: {}", OpenCLManager.getAvailabilityString());
            } catch (Throwable t) {
                LOGGER.warn("Legacy OpenCL init failed, will rely on Quantified GPU backend", t);
            }
        }

        // Initialize Quantified GPU backend router (Vulkan > OpenCL > CPU)
        try {
            GpuNoiseBackend.initialize();
            LOGGER.info("GPU noise backend: {}", GpuNoiseBackend.getStatusString());
        } catch (Throwable t) {
            LOGGER.warn("Quantified GPU backend init failed, using legacy OpenCL", t);
        }

        // Trigger probes to discover GPU capabilities
        try {
            QuantifiedGpuRuntime.triggerGpuProbes("gpunoise-module-init");
        } catch (Throwable ignored) {
        }

        // Initialize terrain cache
        try {
            TerrainCache.initialize();
        } catch (Throwable t) {
            LOGGER.warn("Terrain cache init failed (non-critical): {}", t.getMessage());
        }

        // Initialize hybrid noise dispatcher (SIMD + GPU synergy)
        try {
            HybridNoiseDispatcher.initialize();
        } catch (Throwable t) {
            LOGGER.warn("Hybrid noise dispatcher init failed (non-critical): {}", t.getMessage());
        }

        LOGGER.info("GPU Noise module fully initialized");
    }

    private static boolean customVulkanAccelOwnsGpuNoise() {
        try {
            Class<?> accel = Class.forName("org.admany.vkgpuaccel.VkGpuAccel");
            Object result = accel.getMethod("shouldOwnGpuNoise").invoke(null);
            return result instanceof Boolean value && value;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
