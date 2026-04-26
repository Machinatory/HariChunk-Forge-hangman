package com.hari.harichunk.opts.dfc.common.mixin.access;

import net.minecraft.world.level.levelgen.NoiseChunk;

public interface INoiseChunk {

    boolean getIsInInterpolationLoop();

    boolean getIsInterpolating();

    int getStartBlockX();

    int getStartBlockY();

    int getStartBlockZ();

    int getHorizontalCellBlockCount();

    int getVerticalCellBlockCount();

    int getStartBiomeX();

    int getStartBiomeZ();

    /**
     * Helper to cast a NoiseChunk to INoiseChunk.
     */
    static INoiseChunk cast(NoiseChunk noiseChunk) {
        return (INoiseChunk) (Object) noiseChunk;
    }
}
