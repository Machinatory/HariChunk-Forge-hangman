package com.hari.harichunk.opts.dfc.common.ducks;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import net.minecraft.world.level.levelgen.DensityFunction;

public interface IFastCacheLike {
    long CACHE_MISS_NAN_BITS = 0x7ffddb972d486a4fL;

    double harichunk$getCached(int x, int y, int z, EvalType evalType);

    boolean harichunk$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType);

    void harichunk$cache(int x, int y, int z, EvalType evalType, double cached);

    void harichunk$cache(double[] res, int[] x, int[] y, int[] z, EvalType evalType);

    DensityFunction harichunk$getDelegate();

    DensityFunction harichunk$withDelegate(DensityFunction delegate);
}
