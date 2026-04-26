package com.hari.harichunk.base;

import com.hari.harichunk.base.common.ModuleMixinPlugin;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Used internally for harichunk-base, do not subclass.
 */
public final class TheMixinPlugin extends ModuleMixinPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk Base Mixin Plugin");

    // SJhub start - thanks for Arclight
    private final List<Object> postProcessors = createPostProcessors();
    // SJhub end - thanks for Arclight

    private static List<Object> createPostProcessors() {
        try {
            return List.of(Class.forName("io.izzel.arclight.common.mod.mixins.TransformAccessProcessor")
                    .getDeclaredConstructor()
                    .newInstance());
        } catch (Throwable throwable) {
            LOGGER.debug("Arclight TransformAccessProcessor unavailable; skipping post-processors", throwable);
            return List.of();
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) {
            return false;
        }

        if (mixinClassName.startsWith("com.hari.harichunk.base.mixin.util.log4j2shutdownhookisnomore."))
            return ModuleEntryPoint.disableLoggingShutdownHook;

        return true;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // SJhub start - thanks for Arclight
        for (var processor : this.postProcessors) {
            try {
                Method accept = processor.getClass().getMethod("accept", String.class, ClassNode.class, IMixinInfo.class);
                accept.invoke(processor, targetClassName, targetClass, mixinInfo);
            } catch (Throwable throwable) {
                LOGGER.debug("Failed to apply Arclight post-processor {}", processor.getClass().getName(), throwable);
            }
        }
        try {
            Class<?> mixinToolsClass = Class.forName("io.izzel.arclight.mixin.MixinTools");
            Method onPostMixin = mixinToolsClass.getMethod("onPostMixin", ClassNode.class);
            onPostMixin.invoke(null, targetClass);
        } catch (Throwable throwable) {
            LOGGER.debug("Arclight MixinTools unavailable; skipping post-mixin hook", throwable);
        }
        // SJhub end - thanks for Arclight
    }
}
