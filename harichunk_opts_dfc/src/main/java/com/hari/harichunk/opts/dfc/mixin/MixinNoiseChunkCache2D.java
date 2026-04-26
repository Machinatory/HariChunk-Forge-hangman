package com.hari.harichunk.opts.dfc.mixin;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.ducks.IFastCacheLike;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoiseChunk.Cache2D.class)
public abstract class MixinNoiseChunkCache2D implements IFastCacheLike {

    @Shadow(remap = false) private long f_209285_; // lastPos2D

    @Shadow(remap = false) private double f_209286_; // lastValue

    @Mutable
    @Shadow(remap = false) @Final private DensityFunction f_209284_; // function (delegate)

    @Override
    public double harichunk$getCached(int x, int y, int z, EvalType evalType) {
        long l = ChunkPos.asLong(x, z);
        if (this.f_209285_ == l) {
            return this.f_209286_;
        } else {
            return Double.longBitsToDouble(CACHE_MISS_NAN_BITS);
        }
    }

    @Override
    public boolean harichunk$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        return false;
    }

    @Override
    public void harichunk$cache(int x, int y, int z, EvalType evalType, double cached) {
        this.f_209285_ = ChunkPos.asLong(x, z);
        this.f_209286_ = cached;
    }

    @Override
    public void harichunk$cache(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        // nop
    }

    @Override
    public DensityFunction harichunk$getDelegate() {
        return this.f_209284_;
    }

    @Override
    public DensityFunction harichunk$withDelegate(DensityFunction delegate) {
        this.f_209284_ = delegate;
        return (DensityFunction) (Object) this;
    }
}
