package com.hari.harichunk.opts.allocs;

import com.hari.harichunk.base.common.ModuleMixinPlugin;
import net.minecraftforge.fml.ModList;
import net.sjhub.harichunk.utils.ModUtil;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        if (mixinClassName.equals("com.hari.harichunk.opts.allocs.mixin.MixinNbtCompound") ||
                mixinClassName.equals("com.hari.harichunk.opts.allocs.mixin.MixinNbtCompound1"))
            return !ModUtil.isModLoaded("lithium") &&
                    !ModUtil.isModLoaded("radium") &&
                    !ModUtil.isModLoaded("canary") &&
                    !ModUtil.isModLoaded("harium");

        if (mixinClassName.startsWith("com.hari.harichunk.opts.allocs.mixin.surfacebuilder.")) {
            return !ModUtil.isModLoaded("quilted_fabric_api") &&
                   !ModUtil.isModLoaded("frozenlib");
        }

        return true;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        super.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
//        ASMTransformerNbtOpsMapBuilderFastUtilMap.transform(targetClass); // SJhub - I don't need this on forge
    }

}
