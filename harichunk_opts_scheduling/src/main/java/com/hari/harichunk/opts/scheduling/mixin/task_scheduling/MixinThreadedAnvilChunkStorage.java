package com.hari.harichunk.opts.scheduling.mixin.task_scheduling;

import com.hari.harichunk.opts.scheduling.common.IThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;

@Mixin(ChunkMap.class)
public class MixinThreadedAnvilChunkStorage implements IThreadedAnvilChunkStorage {

    @Shadow(remap = false) @Final private ServerLevel f_140133_; // level
    @Shadow(remap = false) @Final private BlockableEventLoop<Runnable> f_140135_; // mainThreadExecutor

    @Shadow(remap = false) @Final private ChunkMap.DistanceManager f_140145_; // distanceManager
    private final Executor mainInvokingExecutor = runnable -> {
        if (this.f_140133_.getServer().isSameThread()) {
            runnable.run();
        } else {
            this.f_140135_.execute(runnable);
        }
    };

    @Override
    public Executor getMainInvokingExecutor() {
        return mainInvokingExecutor;
    }

    /**
     * reduce scheduling overhead with mainInvokingExecutor
     */
    @Redirect(method = "prepareEntityTickingChunk", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenApplyAsync(Ljava/util/function/Function;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private <U, T> CompletableFuture<U> redirectMainThreadExecutor1(CompletableFuture<T> completableFuture, Function<? super T, ? extends U> fn, Executor executor) {
        return completableFuture.thenApplyAsync(fn, this.mainInvokingExecutor);
    }

//    /**
//     * @author Hari
//     * @reason reduce scheduling overhead with mainInvokingExecutor
//     */
//    @Overwrite
//    public void releaseLightTicket(ChunkPos pos) {
//        // TODO [VanilaCopy]
//        this.mainInvokingExecutor.execute(Util.debugRunnable(() -> {
//            this.ticketManager.removeTicketWithLevel(ChunkTicketType.LIGHT, pos, 33 + ChunkStatus.getDistanceFromFull(ChunkStatus.LIGHT), pos);
//        }, () -> {
//            return "release light ticket " + pos;
//        }));
//    }

    // private synthetic method_17252(Lnet/minecraft/server/world/ChunkHolder;Ljava/lang/Runnable;)V
    /**
     * TODO lambda expression of convertToFullChunk
     *
     * @author Hari
     * @reason reduce scheduling overhead with mainInvokingExecutor
     */
    @Dynamic
    @Overwrite(remap = false)
    private void m_214949_(ChunkHolder holder, Runnable runnable) {
        this.mainInvokingExecutor.execute(runnable);
    }

    // private synthetic method_19487(Lnet/minecraft/server/world/ChunkHolder;Ljava/lang/Runnable;)V
    /**
     * TODO first lambda expression of makeChunkTickable
     *
     * @author Hari
     * @reason reduce scheduling overhead with mainInvokingExecutor
     */
    @SuppressWarnings("OverwriteTarget")
    @Dynamic
    @Overwrite(remap = false)
    private void m_214942_(ChunkHolder holder, Runnable runnable) {
        this.mainInvokingExecutor.execute(runnable);
    }

    // private synthetic method_19486(Lnet/minecraft/server/world/ChunkHolder;Ljava/lang/Runnable;)V
    /**
     * TODO second lambda expression of makeChunkTickable
     *
     * @author Hari
     * @reason reduce scheduling overhead with mainInvokingExecutor
     */
    @SuppressWarnings("OverwriteTarget")
    @Dynamic
    @Overwrite(remap = false)
    private void m_214920_(ChunkHolder holder, Runnable runnable) {
        this.mainInvokingExecutor.execute(runnable);
    }

    // private synthetic method_20579(Lnet/minecraft/server/world/ChunkHolder;Ljava/lang/Runnable;)V
    /**
     * TODO lambda expression of method_31417 (makeChunkAccessible)
     *
     * @author Hari
     * @reason reduce scheduling overhead with mainInvokingExecutor
     */
    @SuppressWarnings("OverwriteTarget")
    @Dynamic
    @Overwrite(remap = false)
    private void m_214857_(ChunkHolder holder, Runnable runnable) {
        this.mainInvokingExecutor.execute(runnable);
    }

}
