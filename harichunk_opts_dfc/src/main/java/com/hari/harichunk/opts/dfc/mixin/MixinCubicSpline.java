package com.hari.harichunk.opts.dfc.mixin;

import net.minecraft.util.Mth;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.util.CubicSpline;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Mixin(CubicSpline.Multipoint.class)
public abstract class MixinCubicSpline<C, I extends ToFloatFunction<C>> {

    @Shadow(remap = false) @Final private I f_184319_; // coordinate

    @Shadow(remap = false) @Final private float[] f_184320_; // locations

    @Shadow(remap = false) @Final private List<CubicSpline<C, I>> f_184321_; // values

    @Shadow(remap = false) @Final private float[] f_184322_; // derivatives

    /**
     * @author Hari
     * @reason inline binary search and simplify method for MC 1.20.1
     */
    @Overwrite(remap = false)
    public float m_183321_(C x) {
        float point = this.f_184319_.apply(x);
        // Inlined binary search (findRangeForLocation)
        int min = 0;
        int count = this.f_184320_.length;
        while (count > 0) {
            int half = count / 2;
            int mid = min + half;
            if (point < this.f_184320_[mid]) {
                count = half;
            } else {
                min = mid + 1;
                count -= half + 1;
            }
        }
        int rangeForLocation = min - 1;
        // End inlined binary search

        int last = this.f_184320_.length - 1;
        if (rangeForLocation < 0) {
            return this.f_184321_.get(0).apply(x);
        } else if (rangeForLocation == last) {
            return this.f_184321_.get(last).apply(x);
        } else {
            float loc0 = this.f_184320_[rangeForLocation];
            float loc1 = this.f_184320_[rangeForLocation + 1];
            float locDist = loc1 - loc0;
            float k = (point - loc0) / locDist;
            float n = this.f_184321_.get(rangeForLocation).apply(x);
            float o = this.f_184321_.get(rangeForLocation + 1).apply(x);
            float onDist = o - n;
            float p = this.f_184322_[rangeForLocation] * locDist - onDist;
            float q = -this.f_184322_[rangeForLocation + 1] * locDist + onDist;
            return Mth.lerp(k, n, o) + k * (1.0F - k) * Mth.lerp(k, p, q);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CubicSpline.Multipoint<?, ?> that = (CubicSpline.Multipoint<?, ?>) o;
        return Objects.equals(f_184319_, that.coordinate()) && Arrays.equals(f_184320_, that.locations()) && Objects.equals(f_184321_, that.values()) && Arrays.equals(f_184322_, that.derivatives());
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + Objects.hashCode(f_184319_);
        result = 31 * result + Arrays.hashCode(f_184320_);
        result = 31 * result + Objects.hashCode(f_184321_);
        result = 31 * result + Arrays.hashCode(f_184322_);

        return result;
    }
}
