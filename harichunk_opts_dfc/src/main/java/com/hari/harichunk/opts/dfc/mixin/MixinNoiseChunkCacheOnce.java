package com.hari.harichunk.opts.dfc.mixin;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.ducks.IFastCacheLike;
import com.hari.harichunk.opts.dfc.common.vif.ContextProviderVanillaInterface;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;

@Mixin(NoiseChunk.CacheOnce.class)
public abstract class MixinNoiseChunkCacheOnce implements IFastCacheLike {

    @Mutable
    @Shadow(remap = false) @Final private DensityFunction f_209310_; // function (delegate)
    private double harichunk$lastValue = Double.NaN;
    private int harichunk$lastX = Integer.MIN_VALUE;
    private int harichunk$lastY = Integer.MIN_VALUE;
    private int harichunk$lastZ = Integer.MIN_VALUE;

    private int[] harichunk$lastXa;
    private int[] harichunk$lastYa;
    private int[] harichunk$lastZa;
    private double[] harichunk$lastValuea;

    @WrapMethod(method = "compute")
    private double wrapCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        if (context instanceof NoiseChunk) {
            return original.call(context);
        }
        int blockX = context.blockX();
        int blockY = context.blockY();
        int blockZ = context.blockZ();
        if (harichunk$lastValuea != null) {
            for (int i = 0; i < this.harichunk$lastValuea.length; i++) {
                if (harichunk$lastXa[i] == blockX && harichunk$lastYa[i] == blockY && harichunk$lastZa[i] == blockZ) {
                    return harichunk$lastValuea[i];
                }
            }
        }
        if (!Double.isNaN(harichunk$lastValue) && harichunk$lastX == blockX && harichunk$lastY == blockY && harichunk$lastZ == blockZ) {
            return harichunk$lastValue;
        }
        double sample = this.f_209310_.compute(context);
        harichunk$lastValue = sample;
        harichunk$lastX = blockX;
        harichunk$lastY = blockY;
        harichunk$lastZ = blockZ;
        return sample;
    }

    @WrapMethod(method = "fillArray")
    private void wrapFillArray(double[] densities, DensityFunction.ContextProvider provider, Operation<Void> original) {
        if (provider instanceof NoiseChunk) {
            original.call(densities, provider);
            return;
        }
        if (provider instanceof ContextProviderVanillaInterface ap) {
            if (harichunk$lastValuea != null && Arrays.equals(ap.getY(), harichunk$lastYa) && Arrays.equals(ap.getX(), harichunk$lastXa) && Arrays.equals(ap.getZ(), harichunk$lastZa)) {
                System.arraycopy(harichunk$lastValuea, 0, densities, 0, harichunk$lastValuea.length);
            } else {
                this.f_209310_.fillArray(densities, provider);
                this.harichunk$lastValuea = Arrays.copyOf(densities, densities.length);
                this.harichunk$lastXa = ap.getX();
                this.harichunk$lastYa = ap.getY();
                this.harichunk$lastZa = ap.getZ();
            }
            return;
        }
        this.f_209310_.fillArray(densities, provider);
    }

    @Override
    public double harichunk$getCached(int x, int y, int z, EvalType evalType) {
        if (harichunk$lastValuea != null) {
            for (int i = 0; i < this.harichunk$lastValuea.length; i++) {
                if (harichunk$lastXa[i] == x && harichunk$lastYa[i] == y && harichunk$lastZa[i] == z) {
                    return harichunk$lastValuea[i];
                }
            }
        }
        if (!Double.isNaN(harichunk$lastValue) && harichunk$lastX == x && harichunk$lastY == y && harichunk$lastZ == z) {
            return harichunk$lastValue;
        }

        return Double.longBitsToDouble(CACHE_MISS_NAN_BITS);
    }

    @Override
    public boolean harichunk$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        if (harichunk$lastValuea != null && Arrays.equals(y, harichunk$lastYa) && Arrays.equals(x, harichunk$lastXa) && Arrays.equals(z, harichunk$lastZa)) {
            System.arraycopy(harichunk$lastValuea, 0, res, 0, harichunk$lastValuea.length);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void harichunk$cache(int x, int y, int z, EvalType evalType, double cached) {
        harichunk$lastValue = cached;
        harichunk$lastX = x;
        harichunk$lastY = y;
        harichunk$lastZ = z;
    }

    @Override
    public void harichunk$cache(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        if (harichunk$lastValuea != null && this.harichunk$lastValuea.length == res.length) {
            System.arraycopy(res, 0, this.harichunk$lastValuea, 0, this.harichunk$lastValuea.length);
            System.arraycopy(x, 0, this.harichunk$lastXa, 0, this.harichunk$lastValuea.length);
            System.arraycopy(y, 0, this.harichunk$lastYa, 0, this.harichunk$lastValuea.length);
            System.arraycopy(z, 0, this.harichunk$lastZa, 0, this.harichunk$lastValuea.length);
        } else {
            this.harichunk$lastValuea = Arrays.copyOf(res, res.length);
            this.harichunk$lastXa = Arrays.copyOf(x, x.length);
            this.harichunk$lastYa = Arrays.copyOf(y, y.length);
            this.harichunk$lastZa = Arrays.copyOf(z, z.length);
        }
    }

    @Override
    public DensityFunction harichunk$getDelegate() {
        return this.f_209310_;
    }

    @Override
    public DensityFunction harichunk$withDelegate(DensityFunction delegate) {
        this.f_209310_ = delegate;
        return (DensityFunction) (Object) this;
    }
}
