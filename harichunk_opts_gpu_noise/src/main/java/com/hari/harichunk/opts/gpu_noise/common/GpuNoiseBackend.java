package com.hari.harichunk.opts.gpu_noise.common;

import org.admany.quantifiedintegration.api.compute.GpuBackendPreference;
import org.admany.quantifiedintegration.api.compute.GpuBackendType;
import org.admany.quantifiedintegration.gpu.GpuBackendRouter;
import org.admany.quantifiedintegration.gpu.QuantifiedGpuRuntime;
import org.admany.quantifiedintegration.gpu.thermal.ThermalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes noise computation to the optimal GPU backend (Vulkan > OpenCL > CPU),
 * with thermal awareness for automatic fallback when GPU overheats.
 */
public final class GpuNoiseBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk GPU Noise/Backend");
    private static final String MOD_ID = "harichunk";

    private static volatile GpuBackendType activeBackend = GpuBackendType.CPU;
    private static volatile boolean initialized = false;
    private static final ThermalManager thermalManager = new ThermalManager();

    private GpuNoiseBackend() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        if (customVulkanAccelDisablesLegacyOpenCl()) {
            if (customVulkanAccelCanComputeDensity()) {
                activeBackend = GpuBackendType.VULKAN;
                LOGGER.info("GPU noise backend selected: custom Quantified Vulkan acceleration");
            } else {
                activeBackend = GpuBackendType.CPU;
                LOGGER.info("Custom Vulkan accel is active, but exact density GPU batches are disabled; using CPU fallback instead of legacy OpenCL");
            }
            return;
        }

        GpuBackendRouter.Selection selection = GpuBackendRouter.selectBackend(
                MOD_ID,
                GpuBackendPreference.VULKAN_PREFERRED,
                QuantifiedGpuRuntime.isOpenClAvailable(),
                QuantifiedGpuRuntime.isVulkanAvailable()
        );

        activeBackend = selection.backendType();
        LOGGER.info("GPU noise backend selected: {} (preference: {})", activeBackend, selection.effectivePreference());

        if (activeBackend == GpuBackendType.VULKAN) {
            try {
                if (QuantifiedGpuRuntime.ensureVulkanInitialised()) {
                    LOGGER.info("Vulkan compute backend ready for noise acceleration");
                } else {
                    LOGGER.warn("Vulkan compute init returned unavailable, falling back");
                    fallbackToOpenCL();
                }
            } catch (Exception e) {
                LOGGER.warn("Vulkan compute init failed, falling back: {}", e.getMessage());
                fallbackToOpenCL();
            }
        }
    }

    /**
     * Check if GPU computation should be used for the given batch size,
     * considering thermal state and backend availability.
     */
    public static boolean shouldUseGpu(int batchSize) {
        if (!initialized) {
            initialize();
        }
        if (batchSize < Config.MIN_BATCH_SIZE) return false;

        if (thermalManager.isThermallyLimited()) {
            return false;
        }

        if (customVulkanAccelDisablesLegacyOpenCl()) {
            if (!customVulkanAccelCanComputeDensity()) {
                activeBackend = GpuBackendType.CPU;
                return false;
            }
            activeBackend = GpuBackendType.VULKAN;
            return true;
        }

        return activeBackend != GpuBackendType.CPU;
    }

    /**
     * Get the current active backend type.
     */
    public static GpuBackendType getActiveBackend() {
        return activeBackend;
    }

    /**
     * Check if Vulkan backend is active.
     */
    public static boolean isVulkanActive() {
        return activeBackend == GpuBackendType.VULKAN;
    }

    /**
     * Check if OpenCL backend (either HariChunk's own or Quantified's) is active.
     */
    public static boolean isOpenCLActive() {
        return activeBackend == GpuBackendType.OPENCL;
    }

    /**
     * Get the thermal manager for temperature monitoring integration.
     */
    public static ThermalManager getThermalManager() {
        return thermalManager;
    }

    /**
     * Update thermal state from external temperature readings.
     */
    public static void updateTemperature(double temperatureC) {
        boolean wasLimited = thermalManager.isThermallyLimited();
        thermalManager.updateThermalLimiter(temperatureC);
        if (!wasLimited && thermalManager.isThermallyLimited()) {
            LOGGER.warn("GPU thermal limit reached ({}C), falling back to CPU for noise computation", String.format("%.1f", temperatureC));
        } else if (wasLimited && !thermalManager.isThermallyLimited()) {
            LOGGER.info("GPU thermal recovery, resuming GPU noise computation");
        }
    }

    /**
     * Fallback chain: try Vulkan → OpenCL → CPU
     */
    private static void fallbackToOpenCL() {
        if (customVulkanAccelDisablesLegacyOpenCl()) {
            if (customVulkanAccelCanComputeDensity()) {
                activeBackend = GpuBackendType.VULKAN;
                LOGGER.info("Using custom Quantified Vulkan acceleration for noise computation");
            } else {
                activeBackend = GpuBackendType.CPU;
                LOGGER.info("Legacy OpenCL disabled by custom Vulkan accel; using CPU fallback until exact VK density kernels are enabled");
            }
            return;
        }

        if (QuantifiedGpuRuntime.isOpenClAvailable()) {
            activeBackend = GpuBackendType.OPENCL;
            LOGGER.info("Fell back to OpenCL backend for noise computation");
        } else if (com.hari.harichunk.opts.gpu_noise.common.OpenCLManager.isAvailable()) {
            // HariChunk's own OpenCL manager as last GPU resort
            activeBackend = GpuBackendType.OPENCL;
            LOGGER.info("Fell back to HariChunk OpenCL backend");
        } else {
            activeBackend = GpuBackendType.CPU;
            LOGGER.info("No GPU backend available, using CPU for noise computation");
        }
    }

    public static String getStatusString() {
        if (!initialized) {
            initialize();
        }
        String thermal = thermalManager.isThermallyLimited() ? " [THERMAL LIMIT]" : "";
        return "Backend: " + activeBackend + thermal;
    }

    private static boolean customVulkanAccelDisablesLegacyOpenCl() {
        return invokeCustomVulkanBoolean("shouldDisableLegacyOpenCl");
    }

    private static boolean customVulkanAccelCanComputeDensity() {
        return invokeCustomVulkanBoolean("canComputeDensityBatches");
    }

    private static boolean invokeCustomVulkanBoolean(String methodName) {
        try {
            Class<?> accel = Class.forName("org.admany.vkgpuaccel.VkGpuAccel");
            Object result = accel.getMethod(methodName).invoke(null);
            return result instanceof Boolean value && value;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
