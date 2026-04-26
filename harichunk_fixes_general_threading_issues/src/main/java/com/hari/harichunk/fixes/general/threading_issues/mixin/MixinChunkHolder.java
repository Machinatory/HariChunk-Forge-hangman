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

    @Shadow
    @Final
    public static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> UNLOADED_CHUNK_FUTURE;
    @Shadow
    private int ticketLevel;

    @Shadow
    protected abstract void updateChunkToSave(CompletableFuture<? extends Either<? extends ChunkAccess, ChunkHolder.ChunkLoadingFailure>> then, String thenDesc);

    @Shadow
    @Final
    private AtomicReferenceArray<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> futures;
    @Shadow private CompletableFuture<ChunkAccess> chunkToSave;
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
    @Overwrite
    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getOrScheduleFuture(ChunkStatus targetStatus, ChunkMap chunkStorage) {
        // TODO [VanillaCopy]
        int i = targetStatus.getIndex();
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.futures.get(i);
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
            completableFuture = this.futures.get(i);
            if (completableFuture != null) {
                Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = completableFuture.getNow(null);
                boolean bl = either != null && either.right().isPresent();
                if (!bl) {
                    return completableFuture;
                }
            }
            if (ChunkLevel.generationStatus(this.ticketLevel).isOrAfter(targetStatus)) {
                future = new CompletableFuture<>();
                this.futures.set(i, future);
                // HariChunk - moved down to prevent deadlock
            } else {
                return completableFuture == null ? UNLOADED_CHUNK_FUTURE : completableFuture;
            }
        }

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture2 = chunkStorage.schedule((ChunkHolder) (Object) this, targetStatus);
        // synchronization: see below
        synchronized (this) {
            this.updateChunkToSave(completableFuture2, "schedule " + targetStatus);
        }
        completableFuture2.whenComplete((either, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                return;
            }
            future.complete(either);
        });
        this.futures.set(i, completableFuture2);
        return completableFuture2;
    }

    @Dynamic
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;updateChunkToSave(Ljava/util/concurrent/CompletableFuture;Ljava/lang/String;)V"))
    private void synchronizeCombineSavingFuture(ChunkHolder holder, CompletableFuture<? extends Either<? extends ChunkAccess, ChunkHolder.ChunkLoadingFailure>> then, String thenDesc) {
        synchronized (this) {
            this.updateChunkToSave(then.exceptionally(unused -> null), thenDesc);
        }
    }

    /**
     * @author Hari
     * @reason synchronize
     */
    @Overwrite
    public void addSaveDependency(String string, CompletableFuture<?> completableFuture) {
        synchronized (this) {
            this.chunkToSave = this.chunkToSave.thenCombine(completableFuture.exceptionally(unused -> null), (chunk, object) -> chunk);
        }
    }

}
