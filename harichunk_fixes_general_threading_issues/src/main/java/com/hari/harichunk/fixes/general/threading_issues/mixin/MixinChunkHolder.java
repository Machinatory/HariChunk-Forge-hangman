package com.hari.harichunk.fixes.general.threading_issues.mixin;

import com.mojang.datafixers.util.Either;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder {

    @Shadow(remap = false)
    @Final
    public static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> f_139996_; // UNLOADED_CHUNK_FUTURE
    @Shadow(remap = false)
    private int f_140007_; // ticketLevel

    @Shadow(remap = false)
    protected abstract void m_143017_(CompletableFuture<? extends Either<? extends ChunkAccess, ChunkHolder.ChunkLoadingFailure>> then, String thenDesc); // updateChunkToSave

    @Shadow(remap = false)
    @Final
    private AtomicReferenceArray<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> f_140001_; // futures
    @Shadow(remap = false) private CompletableFuture<ChunkAccess> f_140005_; // chunkToSave
    @Unique
    private Object schedulingMutex = new Object();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.schedulingMutex = new Object();
    }

    /**
     * @author Hari
     * @reason improve handling of async chunk request
     */
    @Overwrite(remap = false)
    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> m_140049_(ChunkStatus targetStatus, ChunkMap chunkStorage) { // getOrScheduleFuture
        // TODO [VanillaCopy]
        int i = targetStatus.getIndex();
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.f_140001_.get(i);
        if (completableFuture != null) {
            Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = completableFuture.getNow(null);
            boolean bl = either != null && either.right().isPresent();
            if (!bl) {
                return completableFuture;
            }
        }

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future;

        synchronized (this.schedulingMutex) {
            // copied from above
            completableFuture = this.f_140001_.get(i);
            if (completableFuture != null) {
                Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = completableFuture.getNow(null);
                boolean bl = either != null && either.right().isPresent();
                if (!bl) {
                    return completableFuture;
                }
            }
            if (ChunkLevel.generationStatus(this.f_140007_).isOrAfter(targetStatus)) {
                future = new CompletableFuture<>();
                this.f_140001_.set(i, future);
                // HariChunk - moved down to prevent deadlock
            } else {
                return completableFuture == null ? f_139996_ : completableFuture;
            }
        }

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture2 = chunkStorage.schedule((ChunkHolder) (Object) this, targetStatus);
        // synchronization: see below
        synchronized (this) {
            this.m_143017_(completableFuture2, "schedule " + targetStatus);
        }
        completableFuture2.whenComplete((either, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                return;
            }
            future.complete(either);
        });
        this.f_140001_.set(i, completableFuture2);
        return completableFuture2;
    }

    @Dynamic
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;updateChunkToSave(Ljava/util/concurrent/CompletableFuture;Ljava/lang/String;)V"))
    private void synchronizeCombineSavingFuture(ChunkHolder holder, CompletableFuture<? extends Either<? extends ChunkAccess, ChunkHolder.ChunkLoadingFailure>> then, String thenDesc) {
        synchronized (this) {
            this.m_143017_(then.exceptionally(unused -> null), thenDesc);
        }
    }

    /**
     * @author Hari
     * @reason synchronize
     */
    @Overwrite(remap = false)
    public void m_200416_(String string, CompletableFuture<?> completableFuture) { // addSaveDependency
        synchronized (this) {
            this.f_140005_ = this.f_140005_.thenCombine(completableFuture.exceptionally(unused -> null), (chunk, object) -> chunk);
        }
    }

}
