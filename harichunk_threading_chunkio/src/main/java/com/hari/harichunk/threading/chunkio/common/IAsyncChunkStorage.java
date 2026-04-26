package com.hari.harichunk.threading.chunkio.common;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

public interface IAsyncChunkStorage {

    CompletableFuture<Optional<CompoundTag>> getNbtAtAsync(ChunkPos pos);

}
