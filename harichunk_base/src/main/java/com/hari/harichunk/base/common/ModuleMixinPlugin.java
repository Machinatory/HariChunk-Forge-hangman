package com.hari.harichunk.base.common;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ModuleMixinPlugin implements IMixinConfigPlugin {

    static final Logger LOGGER = LoggerFactory.getLogger("HariChunk Modules Manager");

    protected boolean isEnabled = true;

    // Cross-mod detection flags - cached for performance
    private static Boolean hasHarium;
    private static Boolean hasHariMT;
    private static Boolean hasHariPlayer;

    public static boolean hasHarium() {
        if (hasHarium == null) {
            hasHarium = isModLoaded("harium") || isModLoaded("lithium");
            if (hasHarium) LOGGER.info("[HariChunk Compat] Detected Harium/Lithium - NbtCompound optimizations deferred");
        }
        return hasHarium;
    }

    public static boolean hasHariMT() {
        if (hasHariMT == null) {
            hasHariMT = isModLoaded("harimt");
            if (hasHariMT) LOGGER.info("[HariChunk Compat] Detected HariMultiThread - Entity threading coordinated");
        }
        return hasHariMT;
    }

    public static boolean hasHariPlayer() {
        if (hasHariPlayer == null) {
            hasHariPlayer = isModLoaded("hariplayer") || isModLoaded("vmp");
            if (hasHariPlayer) LOGGER.info("[HariChunk Compat] Detected HariPlayer/VMP - Chunk sending coordinated");
        }
        return hasHariPlayer;
    }

    private static boolean isModLoaded(String modId) {
        try {
            return FMLLoader.getLoadingModList().getModFileById(modId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        MixinExtrasBootstrap.init();
        LOGGER.info("Initializing {}", mixinPackage);
        final String[] split = mixinPackage.split("\\.");
        final String targetClass = String.join(".", Arrays.copyOf(split, split.length - 1)) + ".ModuleEntryPoint";
        try {
            final Class<?> entryPoint = Class.forName(targetClass);
            try {
                final Field enabled = entryPoint.getDeclaredField("enabled");
                enabled.setAccessible(true);
                isEnabled = (boolean) enabled.get(null);
            } catch (Throwable t) {
                LOGGER.warn("Unable to detect enabled state for module entrypoint: {}", targetClass, t);
            }
        } catch (Throwable t) {
            LOGGER.warn("Error loading module entrypoint: {}", targetClass, t);
        }
        if (!isEnabled) {
            LOGGER.info("Disabling {}", mixinPackage);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return isEnabled;
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
