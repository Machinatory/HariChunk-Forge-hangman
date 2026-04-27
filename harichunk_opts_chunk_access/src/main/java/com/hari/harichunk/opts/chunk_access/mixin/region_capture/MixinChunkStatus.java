package com.hari.harichunk.opts.chunk_access.mixin.region_capture;

import com.hari.harichunk.opts.chunk_access.common.CurrentWorldGenState;
import com.mojang.datafixers.util.Either;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

@Mixin(value = ChunkStatus.class, priority = 990)
public abstract class MixinChunkStatus {

    @Shadow(remap = false) @Final private ChunkStatus.GenerationTask f_62335_; // generationTask

    @Shadow(remap = false) public abstract String toString();

    /**
     * @author Hari
     * @reason capture chunk regions
     */
    @Overwrite(remap = false)
    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> m_280308_(Executor executor,
                                                                                    ServerLevel world,
                                                                                    ChunkGenerator generator,
                                                                                    StructureTemplateManager structureTemplateManager,
                                                                                    ThreadedLevelLightEngine lightingProvider,
                                                                                    Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunkConverter,
                                                                                    List<ChunkAccess> chunks) {
        try {
            final ChunkStatus thiz = (ChunkStatus) (Object) this;
            CurrentWorldGenState.setCurrentRegion(new WorldGenRegion(world,chunks, thiz, -1));
            ChunkAccess chunk = chunks.get(chunks.size() / 2);
            ProfiledDuration finishable = JvmProfiler.INSTANCE.onChunkGenerate(chunk.getPos(), world.dimension(), this.toString());
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.f_62335_.doWork(thiz, executor, world, generator, structureTemplateManager, lightingProvider, fullChunkConverter, chunks, chunk);
            return completableFuture.thenApply((either) -> {
                if (either.left().isPresent()) {
                    if (either.left().get() instanceof ProtoChunk protoChunk && !protoChunk.getStatus().isOrAfter(thiz)) {
                        protoChunk.setStatus(thiz);
                    }
                }

                if (finishable != null) {
                    finishable.finish();
                }
                return either;
            });
        } finally {
            CurrentWorldGenState.clearCurrentRegion();
        }
    }

}
