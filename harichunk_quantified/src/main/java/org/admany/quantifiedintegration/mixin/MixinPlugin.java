package org.admany.quantifiedintegration.mixin;

import com.hari.harichunk.base.common.ModuleMixinPlugin;
import org.admany.quantifiedintegration.config.Config;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) {
            return false;
        }
        return Config.ENABLE_QUANTIFIED;
    }
}
