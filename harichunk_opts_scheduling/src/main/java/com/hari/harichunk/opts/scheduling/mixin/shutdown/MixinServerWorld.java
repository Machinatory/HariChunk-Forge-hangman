package com.hari.harichunk.opts.scheduling.mixin.shutdown;

import com.hari.harichunk.opts.scheduling.common.ITryFlushable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.LockSupport;

@Mixin(ServerLevel.class)
public class MixinServerWorld {

    @Shadow(remap = false) @Final private MinecraftServer f_8548_; // server

    @Shadow(remap = false) @Final private ServerChunkCache f_8547_; // chunkSource

    @Shadow(remap = false) @Final private PersistentEntitySectionManager<Entity> f_143244_; // entityManager

    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;saveAll()V", shift = At.Shift.BEFORE))
    private void replaceEntityFlushLogic(ProgressListener progressListener, boolean flush, boolean savingDisabled, CallbackInfo ci) {
        while (!((ITryFlushable) this.f_143244_).harichunk$tryFlush()) {
            this.f_8548_.pollTask();
            this.f_8547_.pollTask();
            LockSupport.parkNanos("waiting for completion", 10_000_000);
        }
    }

}
