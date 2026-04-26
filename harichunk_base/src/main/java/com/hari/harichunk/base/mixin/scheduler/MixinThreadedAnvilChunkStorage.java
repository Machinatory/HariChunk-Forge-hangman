package com.hari.harichunk.base.mixin.scheduler;

import com.hari.harichunk.base.common.GlobalExecutors;
import com.hari.harichunk.base.common.scheduler.IVanillaChunkManager;
import com.hari.harichunk.base.common.scheduler.SchedulingManager;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public class MixinThreadedAnvilChunkStorage implements IVanillaChunkManager {

    private final SchedulingManager harichunk$schedulingManager = new SchedulingManager(GlobalExecutors.asyncScheduler, GlobalExecutors.GLOBAL_EXECUTOR_PARALLELISM * 2);

    @Override
    public SchedulingManager harichunk$getSchedulingManager() {
        return this.harichunk$schedulingManager;
    }

    @Inject(method = "updateChunkScheduling", at = @At("RETURN"))
    private void onUpdateLevel(long pos, int level, ChunkHolder holder, int i, CallbackInfoReturnable<ChunkHolder> cir) {
        this.harichunk$schedulingManager.updatePriorityFromLevel(pos, level);
    }

}
