package com.hari.harichunk.compat.immersivepetroleum.mixin;

import com.hari.harichunk.base.common.ModuleMixinPlugin;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return super.shouldApplyMixin(targetClassName, mixinClassName);
    }

}
