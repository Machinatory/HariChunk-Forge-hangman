package com.hari.harichunk.threading.worldgen.mixin.cancellation;

import com.mojang.datafixers.util.Either;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder {

    @Shadow
    @Final
    private AtomicReferenceArray<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> futures;

    @Shadow public abstract void broadcastChanges(LevelChunk chunk);

    @Shadow @Final public static Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> UNLOADED_CHUNK;

//    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReferenceArray;get(I)Ljava/lang/Object;"))
//    private <E> E captureWorldGenCancellation(AtomicReferenceArray<E> instance, int i, ThreadedAnvilChunkStorage chunkStorage, Executor executor) {
//        if (instance == this.futuresByStatus) {
//            final CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future = this.futuresByStatus.get(i);
//            if (future == null) return null;
//            future.complete(UNLOADED_CHUNK);
//            return (E) future;
//        } else return instance.get(i);
//    }

}
