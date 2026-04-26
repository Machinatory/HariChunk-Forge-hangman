package com.hari.harichunk.base.mixin.priority;

import com.hari.harichunk.base.common.scheduler.ISyncLoadManager;
import com.hari.harichunk.base.common.scheduler.IVanillaChunkManager;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkManager implements ISyncLoadManager {

    @Shadow
    @Final
    Thread mainThread;

    @Shadow
    protected abstract boolean chunkAbsent(@Nullable ChunkHolder holder, int maxLevel);

    @Shadow
    @Nullable
    protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);

    @Shadow @Final public ChunkMap chunkMap;
    @Unique
    private volatile ChunkPos currentSyncLoadChunk = null;
    @Unique
    private volatile long syncLoadNanos = 0;

    @Dynamic
    @Redirect(method = {"getChunk", "getChunkBlocking"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache$MainThreadExecutor;managedBlock(Ljava/util/function/BooleanSupplier;)V"), require = 0)
    private void beforeAwaitChunk(ServerChunkCache.MainThreadExecutor instance, BooleanSupplier supplier, int x, int z, ChunkStatus leastStatus, boolean create) {
        if (Thread.currentThread() != this.mainThread || supplier.getAsBoolean()) return;

        this.currentSyncLoadChunk = new ChunkPos(x, z);
        syncLoadNanos = System.nanoTime();
        ((IVanillaChunkManager) this.chunkMap).harichunk$getSchedulingManager().setCurrentSyncLoad(this.currentSyncLoadChunk);
        instance.managedBlock(supplier);
    }

    @Inject(method = "getChunk", at = @At("RETURN"))
    private void afterGetChunk(int x, int z, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<ChunkAccess> cir) {
        if (Thread.currentThread() != this.mainThread) return;

        if (this.currentSyncLoadChunk != null) {
            this.currentSyncLoadChunk = null;
//            System.out.println("Sync load took %.2fms".formatted((System.nanoTime() - syncLoadNanos) / 1e6));
            ((IVanillaChunkManager) this.chunkMap).harichunk$getSchedulingManager().setCurrentSyncLoad(null);
        }
    }

    @Override
    public ChunkPos getCurrentSyncLoad() {
        return this.currentSyncLoadChunk;
    }
}
