package com.hari.harichunk.opts.gpu_noise.mixin;

import com.hari.harichunk.base.common.ModuleMixinPlugin;
import com.hari.harichunk.opts.gpu_noise.common.Config;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;
        return Config.ENABLE_GPU_NOISE;
    }
}
