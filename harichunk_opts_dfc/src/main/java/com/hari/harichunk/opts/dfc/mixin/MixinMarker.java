package com.hari.harichunk.opts.dfc.mixin;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.ducks.IFastCacheLike;
import com.hari.harichunk.opts.dfc.common.ducks.IEqualityOverriding;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DensityFunctions.Marker.class)
public abstract class MixinMarker implements IFastCacheLike, IEqualityOverriding {

    @Mutable
    @Shadow(remap = false) @Final private DensityFunction f_208706_; // wrapped

    @Shadow public abstract DensityFunctions.Marker.Type type();

    @Unique
    private Object harichunk$optionalEquality;

    @Override
    public double harichunk$getCached(int x, int y, int z, EvalType evalType) {
        return Double.longBitsToDouble(CACHE_MISS_NAN_BITS);
    }

    @Override
    public boolean harichunk$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        return false;
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
        return this.f_208706_;
    }

    @Override
    public DensityFunction harichunk$withDelegate(DensityFunction delegate) {
        DensityFunctions.Marker marker = new DensityFunctions.Marker(this.type(), delegate);
        ((IEqualityOverriding) (Object) marker).harichunk$overrideEquality(this);
        return marker;
    }

    @Override
    public void harichunk$overrideEquality(Object object) {
        Object inner = object;
        while (true) {
            Object inner1;
            if (inner instanceof IEqualityOverriding e1) {
                inner1 = e1.harichunk$getOverriddenEquality();
            } else {
                inner1 = null;
            }
            if (inner1 == null) {
                this.harichunk$optionalEquality = inner;
                break;
            }
            inner = inner1;
        }
    }

    @Override
    public Object harichunk$getOverriddenEquality() {
        return this.harichunk$optionalEquality;
    }

    @WrapMethod(method = "hashCode")
    private int wrapHashCode(Operation<Integer> original) {
        Object harichunk$optionalEquality1 = this.harichunk$optionalEquality;
        if (harichunk$optionalEquality1 != null) {
            return harichunk$optionalEquality1.hashCode();
        } else {
            return original.call();
        }
    }

    @WrapMethod(method = "equals")
    private boolean wrapEquals(Object that, Operation<Boolean> original) {
        Object a = this.harichunk$getOverriddenEquality();
        Object b;
        if (that instanceof IEqualityOverriding equalityOverriding) {
            b = equalityOverriding.harichunk$getOverriddenEquality();
        } else {
            b = null;
        }
        if (a == null) {
            return original.call(b != null ? b : that);
        } else {
            return a.equals(b != null ? b : that);
        }
    }

}
