package com.hari.harichunk.threading.lighting.mixin;

import net.minecraft.server.level.ThreadedLevelLightEngine;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedLevelLightEngine.class)
public abstract class MixinServerLightingProvider {

    @Shadow public abstract void tryScheduleUpdate();

    @Dynamic
    @Inject(method = "m_215156_", at = @At("RETURN"))
    private void onPostRunTask(CallbackInfo info) {
        this.tryScheduleUpdate(); // Run more tasks
    }

}
