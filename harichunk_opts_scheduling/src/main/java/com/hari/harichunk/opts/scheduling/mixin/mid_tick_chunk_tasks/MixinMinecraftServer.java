package com.hari.harichunk.opts.scheduling.mixin.mid_tick_chunk_tasks;

import com.hari.harichunk.base.mixin.access.IServerChunkManager;
import com.hari.harichunk.opts.scheduling.common.Config;
import com.hari.harichunk.opts.scheduling.common.ServerMidTickTask;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements ServerMidTickTask {

    @Shadow public abstract Iterable<ServerLevel> getAllLevels();

    @Shadow @Final private Thread serverThread;
    @Unique
    private long midTickChunkTasksLastRun = System.nanoTime();

    @Override
    public void executeTasksMidTick(ServerLevel world) {
        if (this.serverThread != Thread.currentThread()) return;
        if (System.nanoTime() - midTickChunkTasksLastRun < Config.midTickChunkTasksInterval) return;
        ((BlockableEventLoop<Runnable>) ((IServerChunkManager) world.getChunkSource()).getMainThreadExecutor()).pollTask();

        AdmanyDagScheduler.executeMidTickTasks(world);

        midTickChunkTasksLastRun = System.nanoTime();
    }

}
