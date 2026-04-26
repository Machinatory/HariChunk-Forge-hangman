package com.hari.harichunk.opts.scheduling.mixin.general_overheads;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Queue;
import net.minecraft.server.level.ChunkMap;

@Mixin(ChunkMap.class)
public class MixinThreadedAnvilChunkStorage {

    @Shadow @Final private Queue<Runnable> unloadQueue;

    @Redirect(method = "processUnloads", at = @At(value = "INVOKE", target = "Ljava/util/Queue;size()I"))
    private int redirectUnloadSize(Queue<?> queue) {
        if (this.unloadQueue == queue) return Integer.MAX_VALUE;
        return queue.size();
    }

}
