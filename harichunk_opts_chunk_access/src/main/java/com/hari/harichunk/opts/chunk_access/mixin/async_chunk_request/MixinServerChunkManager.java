package com.hari.harichunk.opts.chunk_access.mixin.async_chunk_request;

import com.hari.harichunk.base.common.util.CFUtil;
import com.hari.harichunk.opts.chunk_access.common.CurrentWorldGenState;
import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;

@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkManager {

    @Shadow
    @Final
    private Thread mainThread;

    @Shadow
    @Nullable
    protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);

    @Shadow
    @Final
    private DistanceManager distanceManager;

    @Shadow
    protected abstract boolean chunkAbsent(@Nullable ChunkHolder holder, int maxLevel);

    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    public abstract boolean runDistanceManagerUpdates();

    @Shadow @Final public ChunkMap chunkMap;
    @Shadow @Final public ServerChunkCache.MainThreadExecutor mainThreadProcessor;
    private static final TicketType<ChunkPos> ASYNC_LOAD = TicketType.create("async_load", Comparator.comparingLong(ChunkPos::toLong));

    @Inject(method = "getChunk", at = @At("HEAD"), cancellable = true)
    private void onGetChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<ChunkAccess> cir) {
        if (Thread.currentThread() != this.mainThread) {
            cir.setReturnValue(harichunk$getChunkOffThread(chunkX, chunkZ, leastStatus, create));
        }
    }

    @Unique
    @Final
    private ChunkAccess harichunk$getChunkOffThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        final WorldGenRegion currentRegion = CurrentWorldGenState.getCurrentRegion();
        if (currentRegion != null) {
            ChunkAccess chunk = currentRegion.getChunk(chunkX, chunkZ, leastStatus, false);
            if (chunk instanceof ImposterProtoChunk readOnlyChunk) chunk = readOnlyChunk.getWrapped();
            if (chunk != null) return chunk;
        }
        final CompletableFuture<ChunkAccess> chunkLoad = harichunk$getChunkFutureOffThread(chunkX, chunkZ, leastStatus, create);
        assert chunkLoad != null;
        return CFUtil.join(chunkLoad);
    }

    @Unique
    @Final
    @Nullable
    private CompletableFuture<ChunkAccess> harichunk$getChunkFutureOffThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO [VanillaCopy] getChunkFuture
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            long chunkPosLong = chunkPos.toLong();
            int ticketLevel = 33 + ChunkStatus.getDistance(leastStatus);
            ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPosLong);
            boolean doCreate = create && (chunkHolder == null || this.chunkAbsent(chunkHolder, ticketLevel));
            if (doCreate) {
                this.distanceManager.addTicket(ASYNC_LOAD, chunkPos, ticketLevel, chunkPos);
                if (this.chunkAbsent(chunkHolder, ticketLevel)) {
                    ProfilerFiller profiler = this.level.getProfiler();
                    profiler.push("chunkLoad");
                    this.runDistanceManagerUpdates();
                    chunkHolder = this.getVisibleChunkIfPresent(chunkPosLong);
                    profiler.pop();
                    if (this.chunkAbsent(chunkHolder, ticketLevel)) {
                        throw Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
                    }
                }
            }

            final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = this.chunkAbsent(chunkHolder, ticketLevel) ? ChunkHolder.UNLOADED_CHUNK_FUTURE : chunkHolder.getOrScheduleFuture(leastStatus, this.chunkMap);
            if (doCreate && future != null) {
                future.exceptionally(__ -> null).thenRunAsync(() -> {
                    this.distanceManager.removeTicket(ASYNC_LOAD, chunkPos, ticketLevel, chunkPos);
                }, this.mainThreadProcessor);
            }
            return future;
        }, this.mainThreadProcessor).thenCompose(Function.identity()).thenApply(either -> either.map(Function.identity(), unloaded -> {
            if (create) {
                throw Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + unloaded));
            } else {
                return null;
            }
        }));
    }



}
