package com.hari.harichunk.opts.natives_math.mixin;

import com.hari.harichunk.base.mixin.access.ISimplexNoiseSampler;
import com.hari.harichunk.opts.natives_math.common.NativeBindings;
import com.hari.harichunk.opts.natives_math.common.NativeLoader;
import com.hari.harichunk.opts.natives_math.common.NativeStructs;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction")
public class MixinDFTypesEndIslands {

    @Shadow(remap = false) @Final private SimplexNoise f_208627_;  // islandNoise

    @Unique
    private long harichunk$nativePermPtr = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (NativeLoader.available) {
            harichunk$nativePermPtr = NativeStructs.createSimplexPermutation(((ISimplexNoiseSampler) f_208627_).getP());
        }
    }

    /**
     * @author Hari
     * @reason use native end islands sampling
     */
    @Overwrite(remap = false)
    public double m_207386_(DensityFunction.FunctionContext context) {
        if (harichunk$nativePermPtr != 0) {
            return (double) NativeBindings.endIslandsSample(harichunk$nativePermPtr, context.blockX() / 8, context.blockZ() / 8);
        }
        return 0.0;
    }
}
