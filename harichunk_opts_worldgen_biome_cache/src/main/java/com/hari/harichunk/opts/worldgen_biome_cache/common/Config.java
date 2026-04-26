package com.hari.harichunk.opts.worldgen_biome_cache.common;

public final class Config {

    public static boolean enabled = true;
    public static int maxCacheSize = 16384;
    public static int ttlMinutes = 15;

    private Config() {}

    public static void init() {
        enabled = Boolean.parseBoolean(
                System.getProperty("harichunk.biome_cache.enabled", "true"));
        maxCacheSize = Integer.getInteger(
                "harichunk.biome_cache.max_size", 16384);
        ttlMinutes = Integer.getInteger(
                "harichunk.biome_cache.ttl_minutes", 15);
    }
}
