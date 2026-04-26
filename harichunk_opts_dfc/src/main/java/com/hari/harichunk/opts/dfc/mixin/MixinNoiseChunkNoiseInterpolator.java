package com.hari.harichunk.opts.dfc.mixin;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.ducks.IFastCacheLike;
import com.hari.harichunk.opts.dfc.common.mixin.access.INoiseChunk;
import com.hari.harichunk.opts.dfc.common.vif.FunctionContextVanillaInterface;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseChunk.NoiseInterpolator.class)
public abstract class MixinNoiseChunkNoiseInterpolator implements IFastCacheLike {

    @Unique
    private NoiseChunk harichunk$enclosing;

    // SRG field names for NoiseInterpolator corner values (MC 1.20.1)
    // These are remap=false because the refmap generator doesn't produce field mappings for inner classes
    @Shadow(remap = false) private double f_188831_; // x0y0z0
    @Shadow(remap = false) private double f_188832_; // x1y0z0
    @Shadow(remap = false) private double f_188833_; // x0y1z0
    @Shadow(remap = false) private double f_188834_; // x1y1z0
    @Shadow(remap = false) private double f_188835_; // x0y0z1
    @Shadow(remap = false) private double f_188836_; // x1y0z1
    @Shadow(remap = false) private double f_188837_; // x0y1z1
    @Shadow(remap = false) private double f_188838_; // x1y1z1
    @Shadow(remap = false) private double f_188839_; // result

    @Mutable
    @Shadow(remap = false) @Final private DensityFunction f_188830_; // delegate

    @Inject(method = "<init>", at = @At("RETURN"))
    private void harichunk$captureEnclosing(NoiseChunk enclosing, DensityFunction delegate, CallbackInfo ci) {
        this.harichunk$enclosing = enclosing;
    }

    @WrapMethod(method = "compute")
    private double wrapCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        if (context instanceof NoiseChunk) {
            return original.call(context);
        }
        if (context instanceof FunctionContextVanillaInterface vif && vif.getType() == EvalType.INTERPOLATION) {
            INoiseChunk accessor = INoiseChunk.cast(this.harichunk$enclosing);
            boolean isInInterpolationLoop = accessor.getIsInInterpolationLoop();
            if (!isInInterpolationLoop) {
                return original.call(context);
            }
            boolean isInterpolating = accessor.getIsInterpolating();
            if (!isInterpolating) {
                int startBlockX = accessor.getStartBlockX();
                int startBlockY = accessor.getStartBlockY();
                int startBlockZ = accessor.getStartBlockZ();
                int horizontalCellBlockCount = accessor.getHorizontalCellBlockCount();
                int verticalCellBlockCount = accessor.getVerticalCellBlockCount();
                int cellBlockX = context.blockX() - startBlockX;
                int cellBlockY = context.blockY() - startBlockY;
                int cellBlockZ = context.blockZ() - startBlockZ;
                return Mth.lerp3(
                        (double) cellBlockX / (double) horizontalCellBlockCount,
                        (double) cellBlockY / (double) verticalCellBlockCount,
                        (double) cellBlockZ / (double) horizontalCellBlockCount,
                        this.f_188831_,
                        this.f_188832_,
                        this.f_188833_,
                        this.f_188834_,
                        this.f_188835_,
                        this.f_188836_,
                        this.f_188837_,
                        this.f_188838_
                );
            } else {
                return this.f_188839_;
            }
        }
        return original.call(context);
    }

    @Override
    public double harichunk$getCached(int x, int y, int z, EvalType evalType) {
        if (evalType == EvalType.INTERPOLATION) {
            INoiseChunk accessor = INoiseChunk.cast(this.harichunk$enclosing);
            boolean isInInterpolationLoop = accessor.getIsInInterpolationLoop();
            if (isInInterpolationLoop) {
                boolean isInterpolating = accessor.getIsInterpolating();
                if (!isInterpolating) {
                    int startBlockX = accessor.getStartBlockX();
                    int startBlockY = accessor.getStartBlockY();
                    int startBlockZ = accessor.getStartBlockZ();
                    int horizontalCellBlockCount = accessor.getHorizontalCellBlockCount();
                    int verticalCellBlockCount = accessor.getVerticalCellBlockCount();
                    int cellBlockX = x - startBlockX;
                    int cellBlockY = y - startBlockY;
                    int cellBlockZ = z - startBlockZ;
                    return Mth.lerp3(
                            (double) cellBlockX / (double) horizontalCellBlockCount,
                            (double) cellBlockY / (double) verticalCellBlockCount,
                            (double) cellBlockZ / (double) horizontalCellBlockCount,
                            this.f_188831_,
                            this.f_188832_,
                            this.f_188833_,
                            this.f_188834_,
                            this.f_188835_,
                            this.f_188836_,
                            this.f_188837_,
                            this.f_188838_
                    );
                } else {
                    return this.f_188839_;
                }
            } else {
                throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
            }
        }

        return Double.longBitsToDouble(CACHE_MISS_NAN_BITS);
    }

    @Override
    public boolean harichunk$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        if (evalType == EvalType.INTERPOLATION) {
            INoiseChunk accessor = INoiseChunk.cast(this.harichunk$enclosing);
            boolean isInInterpolationLoop = accessor.getIsInInterpolationLoop();
            if (isInInterpolationLoop) {
                boolean isInterpolating = accessor.getIsInterpolating();
                if (!isInterpolating) {
                    int startBlockX = accessor.getStartBlockX();
                    int startBlockY = accessor.getStartBlockY();
                    int startBlockZ = accessor.getStartBlockZ();
                    double horizontalCellBlockCount = accessor.getHorizontalCellBlockCount();
                    double verticalCellBlockCount = accessor.getVerticalCellBlockCount();
                    for (int i = 0; i < res.length; i++) {
                        int cellBlockX = x[i] - startBlockX;
                        int cellBlockY = y[i] - startBlockY;
                        int cellBlockZ = z[i] - startBlockZ;
                        res[i] = Mth.lerp3(
                                (double) cellBlockX / horizontalCellBlockCount,
                                (double) cellBlockY / verticalCellBlockCount,
                                (double) cellBlockZ / horizontalCellBlockCount,
                                this.f_188831_,
                                this.f_188832_,
                                this.f_188833_,
                                this.f_188834_,
                                this.f_188835_,
                                this.f_188836_,
                                this.f_188837_,
                                this.f_188838_
                        );
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }

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
        return this.f_188830_;
    }

    @Override
    public DensityFunction harichunk$withDelegate(DensityFunction delegate) {
        return (DensityFunction) (Object) this;
    }
}
