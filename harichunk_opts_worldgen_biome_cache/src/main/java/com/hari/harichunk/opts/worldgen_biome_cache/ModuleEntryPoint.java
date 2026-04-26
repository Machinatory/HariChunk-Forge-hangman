package com.hari.harichunk.opts.worldgen_biome_cache;

import com.hari.harichunk.opts.worldgen_biome_cache.common.BiomeCache;
import com.hari.harichunk.opts.worldgen_biome_cache.common.Config;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("harichunk_opts_worldgen_biome_cache")
public class ModuleEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk/BiomeCache");

    static {
        Config.init();
    }

    public ModuleEntryPoint() {
        if (Config.enabled) {
            BiomeCache.initialize();
            LOGGER.info("BiomeCache module initialized [maxSize={}, ttlMinutes={}]",
                    Config.maxCacheSize, Config.ttlMinutes);
        } else {
            LOGGER.info("BiomeCache module disabled by config");
        }
    }

}
