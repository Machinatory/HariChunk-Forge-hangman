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

    @Shadow(remap = false)
    @Final
    private Thread f_8330_; // mainThread

    @Shadow(remap = false)
    @Nullable
    protected abstract ChunkHolder m_8364_(long pos); // getVisibleChunkIfPresent

    @Shadow(remap = false)
    @Final
    private DistanceManager f_8327_; // distanceManager

    @Shadow(remap = false)
    protected abstract boolean m_8416_(@Nullable ChunkHolder holder, int maxLevel); // chunkAbsent

    @Shadow(remap = false)
    @Final
    ServerLevel f_8329_; // level

    @Shadow(remap = false)
    public abstract boolean m_8489_(); // runDistanceManagerUpdates

    @Shadow(remap = false) @Final public ChunkMap f_8325_; // chunkMap
    @Shadow(remap = false) @Final public ServerChunkCache.MainThreadExecutor f_8332_; // mainThreadProcessor
    private static final TicketType<ChunkPos> ASYNC_LOAD = TicketType.create("async_load", Comparator.comparingLong(ChunkPos::toLong));

    @Inject(method = "getChunk", at = @At("HEAD"), cancellable = true)
    private void onGetChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<ChunkAccess> cir) {
        if (Thread.currentThread() != this.f_8330_) {
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
            ChunkHolder chunkHolder = this.m_8364_(chunkPosLong);
            boolean doCreate = create && (chunkHolder == null || this.m_8416_(chunkHolder, ticketLevel));
            if (doCreate) {
                this.f_8327_.addTicket(ASYNC_LOAD, chunkPos, ticketLevel, chunkPos);
                if (this.m_8416_(chunkHolder, ticketLevel)) {
                    ProfilerFiller profiler = this.f_8329_.getProfiler();
                    profiler.push("chunkLoad");
                    this.m_8489_();
                    chunkHolder = this.m_8364_(chunkPosLong);
                    profiler.pop();
                    if (this.m_8416_(chunkHolder, ticketLevel)) {
                        throw Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
                    }
                }
            }

            final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = this.m_8416_(chunkHolder, ticketLevel) ? ChunkHolder.UNLOADED_CHUNK_FUTURE : chunkHolder.getOrScheduleFuture(leastStatus, this.f_8325_);
            if (doCreate && future != null) {
                future.exceptionally(__ -> null).thenRunAsync(() -> {
                    this.f_8327_.removeTicket(ASYNC_LOAD, chunkPos, ticketLevel, chunkPos);
                }, this.f_8332_);
            }
            return future;
        }, this.f_8332_).thenCompose(Function.identity()).thenApply(either -> either.map(Function.identity(), unloaded -> {
            if (create) {
                throw Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + unloaded));
            } else {
                return null;
            }
        }));
    }



}
