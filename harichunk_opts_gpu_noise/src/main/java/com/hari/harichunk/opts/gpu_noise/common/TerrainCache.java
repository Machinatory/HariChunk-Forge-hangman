package com.hari.harichunk.opts.gpu_noise.common;

import org.admany.quantifiedintegration.cache.SimpleThreadSafeCache;
import org.admany.quantifiedintegration.cache.ThreadSafeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Caches computed noise column data keyed by chunk coordinates.
 * Uses the Quantified API bridge cache so this module does not depend on
 * Quantified API internals.
 */
public final class TerrainCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk GPU Noise/Cache");
    private static final String CACHE_NAME = "harichunk_noise_columns";

    private static volatile ThreadSafeCache<NoiseColumnKey, double[]> cache;
    private static volatile boolean enabled = Config.ENABLE_TERRAIN_CACHE;

    private TerrainCache() {}

    public static void initialize() {
        if (!enabled) {
            LOGGER.info("Terrain cache disabled by config");
            return;
        }

        Duration ttl = Duration.ofMinutes(Config.TERRAIN_CACHE_TTL_MINUTES);
        cache = SimpleThreadSafeCache.create(Config.TERRAIN_CACHE_MAX_SIZE, ttl);
        LOGGER.info("Terrain cache initialized (maxSize: {}, TTL: {}min)",
                Config.TERRAIN_CACHE_MAX_SIZE, Config.TERRAIN_CACHE_TTL_MINUTES);
    }

    /**
     * Get cached noise column for the given chunk coordinates, or compute and cache it.
     */
    public static double[] getOrCompute(int chunkX, int chunkZ, int columnHeight, java.util.function.Supplier<double[]> computer) {
        if (!enabled || cache == null) {
            return computer.get();
        }

        NoiseColumnKey key = new NoiseColumnKey(chunkX, chunkZ, columnHeight);
        return cache.get(key, k -> computer.get());
    }

    /**
     * Invalidate all cache entries.
     */
    public static void invalidateAll() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    /**
     * Composite key for noise column cache entries.
     */
    public static final class NoiseColumnKey {
        private final int chunkX;
        private final int chunkZ;
        private final int columnHeight;

        public NoiseColumnKey(int chunkX, int chunkZ, int columnHeight) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.columnHeight = columnHeight;
        }

        @Override
        public int hashCode() {
            return 31 * (31 * chunkX + chunkZ) + columnHeight;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof NoiseColumnKey other)) return false;
            return chunkX == other.chunkX && chunkZ == other.chunkZ && columnHeight == other.columnHeight;
        }

        @Override
        public String toString() {
            return "NoiseColumn[" + chunkX + ", " + chunkZ + ", h=" + columnHeight + "]";
        }
    }
}
