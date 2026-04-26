package com.hari.harichunk.client.uncapvd.mixin;

import com.hari.harichunk.client.uncapvd.common.Config;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinGameOptions {

    @Shadow @Final private OptionInstance<Integer> renderDistance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        final OptionInstance.IntRange callbacks = new OptionInstance.IntRange(2, Config.maxViewDistance);
        ((ISimpleOption<Integer>) (Object) this.renderDistance).setCallbacks(callbacks);
        ((ISimpleOption<Integer>) (Object) this.renderDistance).setCodec(callbacks.codec());
    }

}
