package com.hari.harichunk.notickvd;

import com.hari.harichunk.base.common.ModuleMixinPlugin;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixin plugin for harichunk_notickvd module with cross-mod compatibility.
 * Handles conflicts with HariPlayer/VMP which also overwrites the chunk sending method.
 */
public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        // HariPlayer/VMP compat: both mods @Overwrite the chunk sending method (method_17243 / m_214908_)
        // HariPlayer's AreaPlayerChunkWatchingManager handles chunk sending more comprehensively
        // and already has a compat mixin (MixinNoTickChunkSendingInterceptor) for HariChunk's notickvd
        if (mixinClassName.equals("com.hari.harichunk.notickvd.mixin.MixinThreadedAnvilChunkStorage")) {
            return !hasHariPlayer();
        }

        return true;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        super.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
    }
}
