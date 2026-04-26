package com.hari.harichunk.opts.natives_math.mixin;

import com.hari.harichunk.opts.natives_math.common.NativeBindings;
import com.hari.harichunk.opts.natives_math.common.NativeLoader;
import com.hari.harichunk.opts.natives_math.common.NativeStructs;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlendedNoise.class)
public class MixinInterpolatedNoiseSampler {

    @Unique
    private long harichunk$nativePtr = 0;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (NativeLoader.available) {
            try {
                BlendedNoise self = (BlendedNoise) (Object) this;
                if (NativeStructs.isSpecializedBase3dNoiseFunction(self)) {
                    harichunk$nativePtr = NativeStructs.createInterpolatedNoiseSampler(self);
                }
            } catch (Throwable t) {
                // fallback to vanilla
            }
        }
    }

    /**
     * Use native interpolated noise sampling when available.
     * Falls back to vanilla by not cancelling when native code is unavailable.
     */
    @Inject(method = "compute", at = @At("HEAD"), cancellable = true)
    private void onCompute(DensityFunction.FunctionContext context, CallbackInfoReturnable<Double> cir) {
        if (harichunk$nativePtr != 0) {
            cir.setReturnValue(NativeBindings.noiseInterpolated(harichunk$nativePtr, context.blockX() / 8.0, context.blockY() / 8.0, context.blockZ() / 8.0));
        }
    }
}
