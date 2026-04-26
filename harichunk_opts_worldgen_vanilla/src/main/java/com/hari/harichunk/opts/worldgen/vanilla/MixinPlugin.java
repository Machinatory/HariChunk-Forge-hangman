package com.hari.harichunk.opts.worldgen.vanilla;

import com.hari.harichunk.base.common.ModuleMixinPlugin;
import com.hari.harichunk.opts.worldgen.vanilla.common.Config;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        if (mixinClassName.startsWith("com.hari.harichunk.opts.worldgen.vanilla.mixin.aquifer."))
            return Config.optimizeAquifer;

        if (mixinClassName.startsWith("com.hari.harichunk.opts.worldgen.vanilla.mixin.the_end_biome_cache."))
            return Config.useEndBiomeCache;

        return true;
    }
}
