package com.hari.harichunk.opts.scheduling.mixin.shutdown;

import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Shadow private long nextTickTime;

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;waitUntilNextTick()V", shift = At.Shift.BEFORE))
    private void shutdownBeforeRunTasks(CallbackInfo ci) {
        this.nextTickTime = Util.getMillis() + 100L;
    }

}
