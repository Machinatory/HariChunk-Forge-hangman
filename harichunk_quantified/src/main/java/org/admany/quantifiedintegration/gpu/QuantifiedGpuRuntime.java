package org.admany.quantifiedintegration.gpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public final class QuantifiedGpuRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger("Quantified API Integration/GPU");

    private QuantifiedGpuRuntime() {
    }

    public static boolean hasVulkanBindings() {
        return invokeBoolean("org.admany.quantified.core.common.gpu.backend.VulkanRuntime", "hasBindings", false);
    }

    public static boolean isVulkanAvailable() {
        return invokeBoolean("org.admany.quantified.core.common.gpu.backend.VulkanRuntime", "isAvailable", false);
    }

    public static boolean isOpenClAvailable() {
        return invokeBoolean("org.admany.quantified.core.common.opencl.core.OpenCLManager", "isAvailable", false);
    }

    public static boolean ensureVulkanInitialised() {
        return invokeBoolean("org.admany.quantified.core.common.vulkan.core.VulkanManager", "ensureInitialised", false);
    }

    public static void triggerGpuProbes(String reason) {
        invokeVoid("org.admany.quantified.core.common.opencl.gpu.AsyncProbeScheduler", "triggerProbe", reason);
        if (hasVulkanBindings()) {
            invokeVoid("org.admany.quantified.core.common.gpu.backend.VulkanProbeScheduler", "triggerProbe", reason);
        }
    }

    private static boolean invokeBoolean(String className, String methodName, boolean fallback) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod(methodName);
            Object result = method.invoke(null);
            return result instanceof Boolean value ? value : fallback;
        } catch (Throwable throwable) {
            LOGGER.debug("QAPI GPU runtime method unavailable: {}#{} ({})", className, methodName, throwable.toString());
            return fallback;
        }
    }

    private static void invokeVoid(String className, String methodName, String arg) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod(methodName, String.class);
            method.invoke(null, arg);
        } catch (Throwable throwable) {
            LOGGER.debug("QAPI GPU probe method unavailable: {}#{} ({})", className, methodName, throwable.toString());
        }
    }
}
