package com.hari.harichunk.base.common.theinterface;

import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.CompletableFuture;

public interface IDirectStorage {

    public CompletableFuture<Void> setRawChunkData(ChunkPos pos, byte[] data);

}
