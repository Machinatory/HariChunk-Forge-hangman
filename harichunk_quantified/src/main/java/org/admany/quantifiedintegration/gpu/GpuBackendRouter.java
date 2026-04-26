package org.admany.quantifiedintegration.gpu;

import org.admany.quantifiedintegration.api.compute.GpuBackendPreference;
import org.admany.quantifiedintegration.api.compute.GpuBackendType;

public final class GpuBackendRouter {

    private GpuBackendRouter() {
    }

    public static Selection selectBackend(String modId,
                                          GpuBackendPreference preference,
                                          boolean openClAvailable,
                                          boolean vulkanAvailable) {
        GpuBackendPreference effective = preference == null ? GpuBackendPreference.AUTO : preference;
        if (effective.isCpuOnly()) {
            return new Selection(GpuBackendType.CPU, effective);
        }

        if (effective.requiresVulkan()) {
            return new Selection(vulkanAvailable ? GpuBackendType.VULKAN : GpuBackendType.CPU, effective);
        }
        if (effective.requiresOpenCL()) {
            return new Selection(openClAvailable ? GpuBackendType.OPENCL : GpuBackendType.CPU, effective);
        }

        if (effective.prefersOpenCL()) {
            if (openClAvailable) {
                return new Selection(GpuBackendType.OPENCL, effective);
            }
            if (vulkanAvailable) {
                return new Selection(GpuBackendType.VULKAN, effective);
            }
            return new Selection(GpuBackendType.CPU, effective);
        }

        if (vulkanAvailable) {
            return new Selection(GpuBackendType.VULKAN, effective);
        }
        if (openClAvailable) {
            return new Selection(GpuBackendType.OPENCL, effective);
        }
        return new Selection(GpuBackendType.CPU, effective);
    }

    public record Selection(GpuBackendType backendType, GpuBackendPreference effectivePreference) {
    }
}
