package com.hari.harichunk.opts.natives_math.mixin;

import com.hari.harichunk.opts.natives_math.common.INativePointer;
import com.hari.harichunk.opts.natives_math.common.NativeLoader;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PerlinNoise.class)
public class MixinDoublePerlinNoiseSampler implements INativePointer {

    @Unique
    private long harichunk$nativePtr = 0;

    @Shadow(remap = false) @Final private double f_75393_;  // lowestFreqInputFactor
    @Shadow(remap = false) @Final private double f_75392_;  // lowestFreqValueFactor

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (NativeLoader.available) {
            try {
                // Create native struct for this octave sampler pair
                // PerlinNoise in Forge is the octave sampler - we create struct for its data
                // For now, native acceleration works through the interpolated noise path
            } catch (Throwable t) {
                // fallback to vanilla
            }
        }
    }

    @Override
    public long harichunk$getPointer() {
        return harichunk$nativePtr;
    }
}
