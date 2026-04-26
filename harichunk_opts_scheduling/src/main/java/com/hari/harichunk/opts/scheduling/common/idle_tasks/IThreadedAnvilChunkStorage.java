package com.hari.harichunk.opts.scheduling.common.idle_tasks;

import net.minecraft.world.level.ChunkPos;

public interface IThreadedAnvilChunkStorage {

    void enqueueDirtyChunkPosForAutoSave(ChunkPos chunkPos);

    boolean runOneChunkAutoSave();

}
