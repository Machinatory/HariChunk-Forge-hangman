package com.hari.harichunk.threading.chunkio;

import com.hari.harichunk.base.common.ModuleMixinPlugin;
import net.sjhub.harichunk.utils.ModUtil;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        if (mixinClassName.startsWith("com.hari.harichunk.threading.chunkio.mixin.gc_free_serializer.")) {
            return ModUtil.isModLoaded("harichunk_rewrites_chunk_serializer");
        }

        return true;
    }
}
