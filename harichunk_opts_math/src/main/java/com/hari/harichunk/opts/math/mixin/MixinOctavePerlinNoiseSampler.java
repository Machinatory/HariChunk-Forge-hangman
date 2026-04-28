package com.hari.harichunk.opts.math.mixin;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PerlinNoise.class)
public class MixinOctavePerlinNoiseSampler {

    @Shadow(remap = false) @Final private double f_75393_;  // lowestFreqInputFactor

    @Shadow(remap = false) @Final private double f_75392_;  // lowestFreqValueFactor

    @Shadow(remap = false) @Final private ImprovedNoise[] f_75390_;  // noiseLevels

    @Shadow(remap = false) @Final private DoubleList f_75391_;  // amplitudes

    @Unique
    private int octaveSamplersCount = 0;

    @Unique
    private double[] amplitudesArray = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.octaveSamplersCount = this.f_75390_.length;
        this.amplitudesArray = this.f_75391_.toDoubleArray();
    }

    /**
     * @author Hari
     * @reason remove frequent type conversion
     */
    @Overwrite(remap = false)
    public static double m_75406_(double value) {
        return value - Math.floor(value / 3.3554432E7 + 0.5) * 3.3554432E7;
    }

    /**
     * @author Hari
     * @reason optimize for common cases
     */
    @Overwrite(remap = false)
    public double m_75408_(double x, double y, double z) {
        double d = 0.0;
        double e = this.f_75393_;
        double f = this.f_75392_;

        for(int i = 0; i < this.octaveSamplersCount; ++i) {
            ImprovedNoise perlinNoiseSampler = this.f_75390_[i];
            if (perlinNoiseSampler != null) {
                @SuppressWarnings("deprecation")
                double g = perlinNoiseSampler.noise(
                        m_75406_(x * e), m_75406_(y * e), m_75406_(z * e), 0.0, 0.0
                );
                d += this.amplitudesArray[i] * g * f;
            }

            e *= 2.0;
            f /= 2.0;
        }

        return d;
    }

}
