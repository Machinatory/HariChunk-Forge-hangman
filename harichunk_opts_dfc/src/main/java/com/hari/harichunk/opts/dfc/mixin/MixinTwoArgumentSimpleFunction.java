package com.hari.harichunk.opts.dfc.mixin;

import com.hari.harichunk.opts.dfc.common.ducks.IArrayCacheCapable;
import com.hari.harichunk.opts.dfc.common.util.ArrayCache;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DensityFunctions.Ap2.class)
public class MixinTwoArgumentSimpleFunction {

    @Shadow(remap = false) @Final private DensityFunctions.TwoArgumentSimpleFunction.Type f_208397_; // type

    @Shadow(remap = false) @Final private DensityFunction f_208398_; // argument1

    @Shadow(remap = false) @Final private DensityFunction f_208399_; // argument2

    @WrapMethod(method = "fillArray")
    private void wrapFillArray(double[] densities, DensityFunction.ContextProvider provider, Operation<Void> original) {
        if (this.f_208397_ == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD) {
            this.f_208398_.fillArray(densities, provider);
            double[] ds;

            ArrayCache arrayCache = provider instanceof IArrayCacheCapable arrayCacheCapable ? arrayCacheCapable.harichunk$getArrayCache() : null;

            if (arrayCache != null) {
                ds = arrayCache.getDoubleArray(densities.length, false);
            } else {
                ds = new double[densities.length];
            }

            this.f_208399_.fillArray(ds, provider);

            for (int i = 0; i < densities.length; i++) {
                densities[i] += ds[i];
            }

            if (arrayCache != null) {
                arrayCache.recycle(ds);
            }
        } else {
            original.call(densities, provider);
        }
    }

}
