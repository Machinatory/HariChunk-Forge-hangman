package com.hari.harichunk.opts.scheduling.mixin.task_scheduling;

import com.hari.harichunk.opts.scheduling.common.DuckChunkHolder;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.core.SectionPos;
import net.minecraft.util.thread.BlockableEventLoop;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkManagerLightBatch {

    @Shadow @Nullable protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);

    @Shadow @Final private ServerChunkCache.MainThreadExecutor mainThreadProcessor;

    /**
     * @author Hari
     * @reason batch light updates with AtomicIntegerArray for reduced scheduling overhead
     */
    @Overwrite
    public void onLightUpdate(LightLayer type, SectionPos pos) {
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(new ChunkPos(pos.getX(), pos.getZ()).toLong());
        if (chunkHolder != null) {
            ((DuckChunkHolder) chunkHolder).harichunk$queueLightSectionDirty(type, pos.getY());
            if (((DuckChunkHolder) chunkHolder).harichunk$shouldScheduleUndirty()) {
                this.mainThreadProcessor.execute(() -> {
                    ((DuckChunkHolder) chunkHolder).harichunk$undirtyLight();
                });
            }
        }
    }

}
