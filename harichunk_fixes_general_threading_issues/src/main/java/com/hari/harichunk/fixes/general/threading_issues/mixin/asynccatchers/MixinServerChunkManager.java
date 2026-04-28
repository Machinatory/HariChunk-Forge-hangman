package com.hari.harichunk.fixes.general.threading_issues.mixin.asynccatchers;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ConcurrentModificationException;
import net.minecraft.server.level.ServerChunkCache;

@Mixin(ServerChunkCache.class)
public class MixinServerChunkManager {

    @Shadow(remap = false) @Final Thread f_8330_;  // mainThread

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;Z)V", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (Thread.currentThread() != this.f_8330_) {
            final ConcurrentModificationException e = new ConcurrentModificationException("Async ticking server chunk manager");
            e.printStackTrace();
            throw e;
        }
    }

}
