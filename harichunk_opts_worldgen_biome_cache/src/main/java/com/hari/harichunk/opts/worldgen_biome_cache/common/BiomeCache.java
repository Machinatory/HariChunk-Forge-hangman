package com.hari.harichunk.opts.worldgen_biome_cache.common;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Caches biome lookup results during world generation.
 *
 * During terrain generation, the same biome positions are queried repeatedly
 * (for noise interpolation, surface building, feature placement, etc.).
 * This cache eliminates redundant biome source lookups by storing results
 * keyed by (biomeSource, x, y, z).
 *
 * Thread safety: Uses ConcurrentHashMap for cross-thread access during
 * parallel world generation. Per-chunk caching uses a simple Long2Object map
 * guarded by the generation thread's sequential access pattern.
 */
public final class BiomeCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk/BiomeCache");

    private static final ConcurrentHashMap<BiomeSourceKey, SourceCache> sourceCaches = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    // Stats
    private static final AtomicLong hits = new AtomicLong();
    private static final AtomicLong misses = new AtomicLong();

    private BiomeCache() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        // Start cleanup thread
        Thread cleanup = new Thread(() -> {
            while (initialized) {
                try {
                    Thread.sleep(60_000L * Config.ttlMinutes);
                    cleanup();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "HariChunk-BiomeCache-Cleanup");
        cleanup.setDaemon(true);
        cleanup.start();

        LOGGER.info("BiomeCache initialized");
    }

    /**
     * Get a cached biome or compute and cache it.
     *
     * @param source the biome source (identity-based caching)
     * @param x block X
     * @param y block Y
     * @param z block Z
     * @param computer fallback biome computation
     * @return the biome holder
     */
    public static Holder<Biome> getOrCompute(BiomeSource source, int x, int y, int z,
                                              java.util.function.Supplier<Holder<Biome>> computer) {
        if (!initialized || !Config.enabled) {
            return computer.get();
        }

        SourceCache cache = sourceCaches.computeIfAbsent(
                new BiomeSourceKey(source), k -> new SourceCache());

        long posKey = posKey(x, y, z);
        Holder<Biome> cached = cache.get(posKey);
        if (cached != null) {
            hits.incrementAndGet();
            return cached;
        }

        misses.incrementAndGet();
        Holder<Biome> result = computer.get();
        cache.put(posKey, result);
        return result;
    }

    /**
     * Get biome for 2D lookup (y-independent, used for column biome queries).
     */
    public static Holder<Biome> getOrCompute2D(BiomeSource source, int x, int z,
                                                java.util.function.Supplier<Holder<Biome>> computer) {
        return getOrCompute(source, x, 0, z, computer);
    }

    /**
     * Get cached noise biome from the biome source's noise biome grid.
     * Uses the quarter-resolution coordinates (noise biome grid).
     *
     * @param source the biome source
     * @param quartX noise biome grid X (blockX >> 2)
     * @param quartY noise biome grid Y (blockY >> 2)
     * @param quartZ noise biome grid Z (blockZ >> 2)
     * @param computer fallback computation
     * @return the biome holder
     */
    public static Holder<Biome> getNoiseBiome(BiomeSource source, int quartX, int quartY, int quartZ,
                                               java.util.function.Supplier<Holder<Biome>> computer) {
        return getOrCompute(source, quartX, quartY, quartZ, computer);
    }

    /**
     * Clear cache for a specific biome source (e.g., when unloading a world).
     */
    public static void invalidateSource(BiomeSource source) {
        sourceCaches.remove(new BiomeSourceKey(source));
    }

    /**
     * Clear all caches.
     */
    public static void invalidateAll() {
        sourceCaches.clear();
        hits.set(0);
        misses.set(0);
    }

    public static long getHits() { return hits.get(); }
    public static long getMisses() { return misses.get(); }
    public static double getHitRate() {
        long total = hits.get() + misses.get();
        return total == 0 ? 0.0 : (double) hits.get() / total;
    }

    public static String getStatsString() {
        return String.format("BiomeCache[sources=%d, hits=%d, misses=%d, rate=%.1f%%]",
                sourceCaches.size(), hits.get(), misses.get(), getHitRate() * 100);
    }

    private static long posKey(int x, int y, int z) {
        return (long) (x & 0x3FFFFFF) << 38 | (long) (z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }

    private static void cleanup() {
        long ttlMs = 60_000L * Config.ttlMinutes;
        long now = System.currentTimeMillis();
        int evicted = 0;

        var iter = sourceCaches.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            SourceCache cache = entry.getValue();
            if (now - cache.getLastAccessTime() > ttlMs) {
                iter.remove();
                evicted++;
            } else if (cache.size() > Config.maxCacheSize) {
                cache.trim(Math.max(1, Config.maxCacheSize / 2));
            }
        }

        if (evicted > 0) {
            LOGGER.debug("BiomeCache cleanup: evicted {} sources", evicted);
        }
    }

    // --- Inner classes ---

    private static final class BiomeSourceKey {
        private final BiomeSource source;
        private final int hash;

        BiomeSourceKey(BiomeSource source) {
            this.source = source;
            this.hash = System.identityHashCode(source);
        }

        @Override
        public int hashCode() { return hash; }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof BiomeSourceKey other && other.source == this.source;
        }
    }

    private static final class SourceCache {
        private final Long2ObjectOpenHashMap<Holder<Biome>> cache = new Long2ObjectOpenHashMap<>();
        private volatile long lastAccessTime = System.currentTimeMillis();

        Holder<Biome> get(long key) {
            lastAccessTime = System.currentTimeMillis();
            synchronized (cache) {
                return cache.get(key);
            }
        }

        void put(long key, Holder<Biome> value) {
            lastAccessTime = System.currentTimeMillis();
            synchronized (cache) {
                if (cache.size() < Config.maxCacheSize) {
                    cache.put(key, value);
                }
            }
        }

        int size() {
            synchronized (cache) {
                return cache.size();
            }
        }

        void trim(int targetSize) {
            synchronized (cache) {
                if (cache.size() > targetSize) {
                    // Remove oldest entries by clearing and re-populating with recent ones
                    var entries = cache.long2ObjectEntrySet();
                    var iter = entries.iterator();
                    int toRemove = cache.size() - targetSize;
                    while (iter.hasNext() && toRemove > 0) {
                        iter.next();
                        iter.remove();
                        toRemove--;
                    }
                }
            }
        }

        long getLastAccessTime() { return lastAccessTime; }
    }
}
