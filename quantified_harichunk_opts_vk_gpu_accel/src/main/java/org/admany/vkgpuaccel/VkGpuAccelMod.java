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

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VkGpuAccelMod.MOD_ID)
public final class VkGpuAccelMod {

    public static final String MOD_ID = "quantified_harichunk_opts_vk_gpu_accel";

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk Vulkan GPU Acceleration By Admany");
    public VkGpuAccelMod() {
        if (!VkGpuAccelConfig.ENABLED) {
            LOGGER.info("Vulkan acceleration disabled by config");
            return;
        }

        VkGpuAccel.initialize();
        LOGGER.info("Vulkan acceleration module loaded [{}]", VkGpuAccel.statusString());
    }
}
