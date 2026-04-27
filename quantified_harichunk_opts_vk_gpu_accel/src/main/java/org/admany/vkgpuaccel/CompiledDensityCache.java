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

import org.admany.quantified.api.vulkan.QuantifiedVulkan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

final class CompiledDensityCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk VK Density Compiler");
    private static final ConcurrentHashMap<String, String> COMPILED = new ConcurrentHashMap<>();

    private CompiledDensityCache() {}

    static String getOrRegister(float[] encodedProgram, int instructionCount) {
        String key = computeKey(encodedProgram, instructionCount);
        if (COMPILED.containsKey(key)) {
            return key;
        }
        COMPILED.computeIfAbsent(key, k -> {
            try {
                byte[] spirv = DensityShaderCompiler.compile(encodedProgram, instructionCount);
                QuantifiedVulkan.registerDensityShader(k, spirv);
                VkGpuAccelStats.recordCompiledShader();
                return k;
            } catch (Throwable t) {
                LOGGER.debug("Density shader compilation failed for key {}: {}", k, t.toString());
                return null;
            }
        });
        return COMPILED.getOrDefault(key, null);
    }

    private static String computeKey(float[] encodedProgram, int instructionCount) {
        int len = Math.min(instructionCount * 4, encodedProgram.length);
        long h = 0x9e3779b97f4a7c15L;
        for (int i = 0; i < len; i++) {
            int bits = Float.floatToRawIntBits(encodedProgram[i]);
            h ^= Integer.toUnsignedLong(bits);
            h = Long.rotateLeft(h, 27) * 0x94d049bb133111ebL + 0xbf58476d1ce4e5b9L;
        }
        h ^= instructionCount;
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h = (h ^ (h >>> 27)) * 0x94d049bb133111ebL;
        h ^= h >>> 31;
        return "hc_density_" + Long.toUnsignedString(h, 16);
    }
}
