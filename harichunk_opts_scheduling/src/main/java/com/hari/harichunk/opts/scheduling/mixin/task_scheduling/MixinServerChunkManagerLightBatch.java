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

    @Shadow(remap = false) @Nullable protected abstract ChunkHolder m_8364_(long pos); // getVisibleChunkIfPresent

    @Shadow(remap = false) @Final private ServerChunkCache.MainThreadExecutor f_8332_; // mainThreadProcessor

    /**
     * @author Hari
     * @reason batch light updates with AtomicIntegerArray for reduced scheduling overhead
     */
    @Overwrite
    public void onLightUpdate(LightLayer type, SectionPos pos) {
        ChunkHolder chunkHolder = this.m_8364_(new ChunkPos(pos.getX(), pos.getZ()).toLong());
        if (chunkHolder != null) {
            ((DuckChunkHolder) chunkHolder).harichunk$queueLightSectionDirty(type, pos.getY());
            if (((DuckChunkHolder) chunkHolder).harichunk$shouldScheduleUndirty()) {
                this.f_8332_.execute(() -> {
                    ((DuckChunkHolder) chunkHolder).harichunk$undirtyLight();
                });
            }
        }
    }

}
