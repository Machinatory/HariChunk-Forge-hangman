package com.hari.harichunk.opts.dfc.mixin;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.ducks.IFastCacheLike;
import com.hari.harichunk.opts.dfc.common.mixin.access.INoiseChunk;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.core.QuartPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseChunk.FlatCache.class)
public abstract class MixinNoiseChunkFlatCache implements IFastCacheLike {

    @Unique
    private NoiseChunk harichunk$enclosing;

    @Shadow(remap = false) @Final private double[][] f_209327_; // cache (double[][] in MC 1.20.1)

    @Mutable
    @Shadow(remap = false) @Final private DensityFunction f_209326_; // delegate

    @Inject(method = "<init>", at = @At("RETURN"))
    private void harichunk$captureEnclosing(NoiseChunk enclosing, DensityFunction delegate, boolean shouldBeCaching, CallbackInfo ci) {
        this.harichunk$enclosing = enclosing;
    }

    @Override
    public double harichunk$getCached(int x, int y, int z, EvalType evalType) {
        int i = QuartPos.fromBlock(x);
        int j = QuartPos.fromBlock(z);
        INoiseChunk accessor = INoiseChunk.cast(this.harichunk$enclosing);
        int k = i - accessor.getStartBiomeX();
        int l = j - accessor.getStartBiomeZ();
        int cacheWidth = this.f_209327_.length;
        if (cacheWidth > 0 && k >= 0 && l >= 0 && k < cacheWidth && l < this.f_209327_[0].length) {
            return this.f_209327_[k][l];
        } else {
            return Double.longBitsToDouble(CACHE_MISS_NAN_BITS);
        }
    }

    @Override
    public boolean harichunk$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        INoiseChunk accessor = INoiseChunk.cast(this.harichunk$enclosing);
        int cacheWidth = this.f_209327_.length;
        if (cacheWidth == 0) return false;
        int cacheHeight = this.f_209327_[0].length;
        for (int i = 0; i < res.length; i++) {
            int i1 = QuartPos.fromBlock(x[i]);
            int j1 = QuartPos.fromBlock(z[i]);
            int k = i1 - accessor.getStartBiomeX();
            int l = j1 - accessor.getStartBiomeZ();
            if (k >= 0 && l >= 0 && k < cacheWidth && l < cacheHeight) {
                res[i] = this.f_209327_[k][l];
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public void harichunk$cache(int x, int y, int z, EvalType evalType, double cached) {
        // nop
    }

    @Override
    public void harichunk$cache(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        // nop
    }

    @Override
    public DensityFunction harichunk$getDelegate() {
        return this.f_209326_;
    }

    @Override
    public DensityFunction harichunk$withDelegate(DensityFunction delegate) {
        // Cannot reassign @Final field with remap=false; fallback to returning a new wrapper
        return (DensityFunction) (Object) this;
    }
}
