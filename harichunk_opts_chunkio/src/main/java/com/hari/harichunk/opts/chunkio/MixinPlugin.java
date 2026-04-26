package com.hari.harichunk.opts.chunkio;

import com.hari.harichunk.base.common.ModuleMixinPlugin;
import com.hari.harichunk.opts.chunkio.common.Config;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        if (mixinClassName.startsWith("com.hari.harichunk.opts.chunkio.mixin.compression.modify_default_chunk_compression"))
            return Config.chunkStreamVersion != -1;

        return true;
    }
}
