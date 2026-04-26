package com.hari.harichunk.tfthreadsafetyaddon;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class TFMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("TFThreadSafetyAddon");
    private boolean twilightForestLoaded = false;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            Class.forName("twilightforest.world.components.biomesources.TFBiomeProvider");
            twilightForestLoaded = true;
            LOGGER.info("Twilight Forest detected, enabling TF thread safety mixins");
        } catch (ClassNotFoundException e) {
            twilightForestLoaded = false;
            LOGGER.info("Twilight Forest not detected, disabling TF thread safety mixins");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return twilightForestLoaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
