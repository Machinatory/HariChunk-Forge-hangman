package com.hari.harichunk.opts.scheduling.mixin.task_scheduling;

import com.hari.harichunk.base.mixin.access.IServerChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.concurrent.Executor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.EntityStorage;

@Mixin(EntityStorage.class)
public class MixinEntityChunkDataAccess {

    @Shadow(remap = false) @Final private ServerLevel f_156538_; // level

    @ModifyArg(method = "loadEntities", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenApplyAsync(Ljava/util/function/Function;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private Executor redirectExecutor(Executor executor) {
        return this.isSameThread() ? executor : ((IServerChunkManager) this.f_156538_.getChunkSource()).getMainThreadExecutor(); // SJhub - fix deadlock when getting entities in chunk
    }

    // SJhub start
    @Unique
    private boolean isSameThread() {
        return Thread.currentThread() == ((IServerChunkManager) this.f_156538_.getChunkSource()).getMainThreadExecutor().getRunningThread();
    }
    // SJhub end

}
