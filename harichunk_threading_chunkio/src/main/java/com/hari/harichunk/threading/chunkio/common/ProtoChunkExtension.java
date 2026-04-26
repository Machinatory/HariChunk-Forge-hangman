package com.hari.harichunk.threading.chunkio.common;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;

public interface ProtoChunkExtension {

    void setBlendingComputeFuture(CompletableFuture<Void> future);

    void setBlendingInfo(ChunkPos pos, List<BitSet> bitSet);

    boolean getNeedBlending();

    void setInitialMainThreadComputeFuture(CompletableFuture<Void> future);
    CompletableFuture<Void> getInitialMainThreadComputeFuture();

}
