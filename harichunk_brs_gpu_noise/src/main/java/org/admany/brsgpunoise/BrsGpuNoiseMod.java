/*
 * HariChunk BlackRift Studios Vulkan GPU Noise by Admany / BlackRift Studios.
 * Licensed strictly under BRSSLA V1.5.
 * This is original BlackRift Studios code and is not related to C2ME in any way.
 *
 * This source may be viewed and contributed to through approved project channels.
 * Modification, copying, redistribution, reuse, or derivative use outside those
 * approved contribution flows is strictly not allowed. C2ME may not copy, modify,
 * reuse, redistribute, or derive from this code.
 */
package org.admany.brsgpunoise;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BrsGpuNoiseMod.MOD_ID)
public final class BrsGpuNoiseMod {

    public static final String MOD_ID = "harichunk_brs_gpu_noise";

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk BRS GPU Noise");

    public BrsGpuNoiseMod() {
        if (!BrsGpuNoiseConfig.ENABLED) {
            LOGGER.info("BRS GPU noise disabled by config");
            return;
        }

        BrsGpuNoise.initialize();
        LOGGER.info("BRS GPU noise module loaded [{}]", BrsGpuNoise.statusString());
    }
}
