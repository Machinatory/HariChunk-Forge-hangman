package com.hari.harichunk.opts.chunk_access.common;

import net.minecraft.server.level.WorldGenRegion;

public class CurrentWorldGenState {

    private static final ThreadLocal<WorldGenRegion> currentRegion = new ThreadLocal<>();

    public static WorldGenRegion getCurrentRegion() {
        return currentRegion.get();
    }

    public static void setCurrentRegion(WorldGenRegion region) {
        currentRegion.set(region);
    }

    public static void clearCurrentRegion() {
        currentRegion.remove();
    }

}
