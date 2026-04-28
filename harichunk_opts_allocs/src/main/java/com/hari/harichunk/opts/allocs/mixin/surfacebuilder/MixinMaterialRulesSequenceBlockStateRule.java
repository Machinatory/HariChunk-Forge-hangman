package com.hari.harichunk.opts.allocs.mixin.surfacebuilder;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;

@Mixin(SurfaceRules.SequenceRule.class)
public class MixinMaterialRulesSequenceBlockStateRule {

    @Shadow(remap = false) @Final private List<SurfaceRules.SurfaceRule> f_189685_;  // rules
    @Unique
    private SurfaceRules.SurfaceRule[] rulesArray;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.rulesArray = this.f_189685_.toArray(SurfaceRules.SurfaceRule[]::new);
    }

    /**
     * @author Hari
     * @reason use array for iteration
     */
    @Overwrite(remap = false)
    public @Nullable BlockState m_183550_(int i, int j, int k) {
        // TODO [VanillaCopy]
        for(SurfaceRules.SurfaceRule blockStateRule : this.rulesArray) {
            BlockState blockState = blockStateRule.tryApply(i, j, k);
            if (blockState != null) {
                return blockState;
            }
        }

        return null;
    }

}
