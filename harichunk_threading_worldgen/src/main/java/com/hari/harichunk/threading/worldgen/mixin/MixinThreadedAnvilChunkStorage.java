package com.hari.harichunk.threading.worldgen.mixin;

import com.hari.harichunk.base.common.scheduler.ThreadLocalWorldGenSchedulingState;
import com.hari.harichunk.threading.worldgen.common.Config;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow(remap = false)
    @Nullable
    protected abstract ChunkHolder m_140327_(long pos);  // getVisibleChunkIfPresent

    @Shadow(remap = false) @Final private BlockableEventLoop<Runnable> f_140135_;  // mainThreadExecutor

    @Shadow(remap = false) private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> f_140130_;  // visibleChunkMap

    @Shadow(remap = false) protected abstract CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> m_280541_(ChunkHolder chunkHolder, int margin, IntFunction<ChunkStatus> distanceToStatus);  // getChunkRangeFuture

    /**
     * @author Hari
     * @reason reduce scheduling overhead
     */
    @SuppressWarnings("OverwriteTarget")
    @Dynamic
    @Overwrite(remap = false)
    private void m_214956_(ChunkHolder chunkHolder, Runnable runnable) { // synthetic method for worldGenExecutor scheduling in upgradeChunk
        runnable.run();
    }

    @Dynamic
    @Inject(method = {"method_17225", "lambda$scheduleChunkGeneration$27", "m_279891_"}, at = @At("HEAD"))
    private void captureUpgradingChunkHolder(CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir, @Local(argsOnly = true) ChunkHolder chunkHolder) {
        ThreadLocalWorldGenSchedulingState.setChunkHolder(chunkHolder);
    }

    @Dynamic
    @Inject(method = {"method_17225", "lambda$scheduleChunkGeneration$27", "m_279891_"}, at = @At("RETURN"))
    private void resetUpgradingChunkHolder(CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        ThreadLocalWorldGenSchedulingState.clearChunkHolder();
    }

    @Dynamic
    @Inject(method = {"method_17225", "lambda$scheduleChunkGeneration$27", "m_279891_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;", shift = At.Shift.BEFORE))
    private void resetUpgradingChunkHolderExceptionally(CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        ThreadLocalWorldGenSchedulingState.clearChunkHolder();
    }

    @Redirect(method = "scheduleChunkGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;m_280541_(Lnet/minecraft/server/level/ChunkHolder;ILjava/util/function/IntFunction;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> redirectGetRegion(ChunkMap instance, ChunkHolder chunkHolder, int margin, IntFunction<ChunkStatus> distanceToStatus) {
        if (instance != (Object) this) throw new IllegalStateException();
        return chunkHolder.getOrScheduleFuture(distanceToStatus.apply(0), (ChunkMap) (Object) this)
                .thenComposeAsync(unused -> this.m_280541_(chunkHolder, margin, distanceToStatus), r -> {
                    if (Config.asyncScheduling) {
                        if (this.f_140135_.isSameThread()) {
                            AdmanyDagScheduler.submitAsync(r, "worldgen-range-future");
                        } else {
                            r.run();
                        }
                    } else {
                        this.f_140135_.execute(r);
                    }
                });
    }

    @Redirect(method = "getChunkRangeFuture", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;getUpdatingChunkIfPresent(J)Lnet/minecraft/server/level/ChunkHolder;"))
    private ChunkHolder redirectGetChunkHolder(ChunkMap instance, long pos) {
        return this.f_140130_.get(pos); // thread-safe
    }

}
