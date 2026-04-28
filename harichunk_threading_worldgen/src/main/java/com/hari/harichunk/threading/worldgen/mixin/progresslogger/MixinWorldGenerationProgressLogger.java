package com.hari.harichunk.threading.worldgen.mixin.progresslogger;

import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoggerChunkProgressListener.class)
public class MixinWorldGenerationProgressLogger {

    @Shadow(remap = false)
    @Final
    private static Logger f_9622_;  // LOGGER
    @Shadow(remap = false)
    @Final
    private int f_9623_;  // maxCount
    private volatile ChunkPos spawnPos = null;
    private volatile int radius = 0;
    private volatile int chunkStatusTransitions = 0;
    private int chunkStatuses = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int radius, CallbackInfo info) {
        ChunkStatus status = ChunkStatus.FULL;
        this.radius = radius;
        chunkStatuses = 0;
        chunkStatusTransitions = 0;
        while ((status = status.getParent()) != ChunkStatus.EMPTY)
            chunkStatuses++;
        chunkStatuses++;
    }

    @Inject(method = "updateSpawnPos", at = @At("RETURN"))
    private void onStart(ChunkPos spawnPos, CallbackInfo ci) {
        this.spawnPos = spawnPos;
    }

    @Inject(method = "onStatusChange", at = @At("HEAD"))
    private void onSetChunkStatus(ChunkPos pos, ChunkStatus status, CallbackInfo ci) {
        if (status != null && (this.spawnPos == null || pos.getChessboardDistance(spawnPos) <= radius)) this.chunkStatusTransitions++;
    }

    /**
     * @author Hari
     * @reason replace impl
     */
    @Overwrite
    public int getProgress() {
        // LOGGER.info("{} / {}", chunkStatusTransitions, totalCount * chunkStatuses);
        return Mth.floor((float) this.chunkStatusTransitions * 100.0F / (float) (this.f_9623_ * chunkStatuses));
    }

}
