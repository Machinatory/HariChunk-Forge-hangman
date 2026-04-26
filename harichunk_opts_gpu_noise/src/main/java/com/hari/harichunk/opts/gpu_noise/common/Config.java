package com.hari.harichunk.opts.gpu_noise.common;

public final class Config {

    public static final boolean ENABLE_GPU_NOISE = Boolean.parseBoolean(
            System.getProperty("harichunk.gpu_noise.enabled", "true")
    );

    public static final int MIN_BATCH_SIZE = Integer.getInteger("harichunk.gpu_noise.min_batch", 64);

    public static final int MAX_BATCH_SIZE = Integer.getInteger("harichunk.gpu_noise.max_batch", 4096);

    // Terrain cache settings
    public static final boolean ENABLE_TERRAIN_CACHE = Boolean.parseBoolean(
            System.getProperty("harichunk.gpu_noise.terrain_cache.enabled", "true")
    );

    public static final long TERRAIN_CACHE_MAX_SIZE = Long.getLong("harichunk.gpu_noise.terrain_cache.max_size", 8192);

    public static final int TERRAIN_CACHE_TTL_MINUTES = Integer.getInteger("harichunk.gpu_noise.terrain_cache.ttl_minutes", 10);

    // Thermal settings
    public static final double THERMAL_LIMIT_C = Double.parseDouble(
            System.getProperty("harichunk.gpu_noise.thermal_limit", "90.0")
    );

    // Hybrid dispatcher settings
    public static final int SIMD_PREFERRED_THRESHOLD = Integer.getInteger(
            "harichunk.hybrid.simd_threshold", 256
    );

    public static final boolean HYBRID_DISPATCH_ENABLED = Boolean.parseBoolean(
            System.getProperty("harichunk.hybrid.enabled", "true")
    );

    private Config() {
    }
}
