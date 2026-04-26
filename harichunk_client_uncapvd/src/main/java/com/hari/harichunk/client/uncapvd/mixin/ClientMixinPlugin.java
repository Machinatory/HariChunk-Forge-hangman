package com.hari.harichunk.client.uncapvd.mixin;

import com.hari.harichunk.base.common.ModuleMixinPlugin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMixinPlugin extends ModuleMixinPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk Client UncapVD");

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) {
            return false;
        }
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            LOGGER.debug("Skipping client-only mixin {} on dedicated server", mixinClassName);
            return false;
        }
        return true;
    }
}
