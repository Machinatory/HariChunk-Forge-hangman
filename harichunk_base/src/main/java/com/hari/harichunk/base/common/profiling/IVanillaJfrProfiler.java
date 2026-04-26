package com.hari.harichunk.base.common.profiling;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public interface IVanillaJfrProfiler {

    public ProfiledDuration startChunkLoadSchedule(ChunkPos chunkPos, ResourceKey<Level> world, String targetStatus);

}
