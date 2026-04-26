package com.hari.harichunk.opts.dfc.mixin;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.ducks.IFastCacheLike;
import com.hari.harichunk.opts.dfc.common.mixin.access.INoiseChunk;
import com.hari.harichunk.opts.dfc.common.vif.FunctionContextVanillaInterface;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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

@Mixin(NoiseChunk.CacheAllInCell.class)
public abstract class MixinNoiseChunkCacheAllInCell implements IFastCacheLike {

    @Unique
    private NoiseChunk harichunk$enclosing;

    // SRG field names for CacheAllInCell (MC 1.20.1)
    @Shadow(remap = false) @Final private double[] f_209298_; // cache (double[])

    @Mutable
    @Shadow(remap = false) @Final private DensityFunction f_209297_; // delegate

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
            int startBlockX = accessor.getStartBlockX();
            int startBlockY = accessor.getStartBlockY();
            int startBlockZ = accessor.getStartBlockZ();
            int horizontalCellBlockCount = accessor.getHorizontalCellBlockCount();
            int verticalCellBlockCount = accessor.getVerticalCellBlockCount();
            int cellBlockX = context.blockX() - startBlockX;
            int cellBlockY = context.blockY() - startBlockY;
            int cellBlockZ = context.blockZ() - startBlockZ;
            return cellBlockX >= 0
                    && cellBlockY >= 0
                    && cellBlockZ >= 0
                    && cellBlockX < horizontalCellBlockCount
                    && cellBlockY < verticalCellBlockCount
                    && cellBlockZ < horizontalCellBlockCount
                    ? this.f_209298_[((verticalCellBlockCount - 1 - cellBlockY) * horizontalCellBlockCount + cellBlockX)
                    * horizontalCellBlockCount
                    + cellBlockZ]
                    : this.f_209297_.compute(context);
        }
        return original.call(context);
    }

    @Override
    public double harichunk$getCached(int x, int y, int z, EvalType evalType) {
        if (evalType == EvalType.INTERPOLATION) {
            INoiseChunk accessor = INoiseChunk.cast(this.harichunk$enclosing);
            boolean isInInterpolationLoop = accessor.getIsInInterpolationLoop();
            if (isInInterpolationLoop) {
                int startBlockX = accessor.getStartBlockX();
                int startBlockY = accessor.getStartBlockY();
                int startBlockZ = accessor.getStartBlockZ();
                int horizontalCellBlockCount = accessor.getHorizontalCellBlockCount();
                int verticalCellBlockCount = accessor.getVerticalCellBlockCount();
                int cellBlockX = x - startBlockX;
                int cellBlockY = y - startBlockY;
                int cellBlockZ = z - startBlockZ;
                if (cellBlockX >= 0 &&
                        cellBlockY >= 0 &&
                        cellBlockZ >= 0 &&
                        cellBlockX < horizontalCellBlockCount &&
                        cellBlockY < verticalCellBlockCount &&
                        cellBlockZ < horizontalCellBlockCount) {
                    return this.f_209298_[((verticalCellBlockCount - 1 - cellBlockY) * horizontalCellBlockCount + cellBlockX)
                            * horizontalCellBlockCount
                            + cellBlockZ];
                }
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
                int startBlockX = accessor.getStartBlockX();
                int startBlockY = accessor.getStartBlockY();
                int startBlockZ = accessor.getStartBlockZ();
                int horizontalCellBlockCount = accessor.getHorizontalCellBlockCount();
                int verticalCellBlockCount = accessor.getVerticalCellBlockCount();
                for (int i = 0; i < res.length; i++) {
                    int cellBlockX = x[i] - startBlockX;
                    int cellBlockY = y[i] - startBlockY;
                    int cellBlockZ = z[i] - startBlockZ;
                    if (cellBlockX >= 0 &&
                            cellBlockY >= 0 &&
                            cellBlockZ >= 0 &&
                            cellBlockX < horizontalCellBlockCount &&
                            cellBlockY < verticalCellBlockCount &&
                            cellBlockZ < horizontalCellBlockCount) {
                        res[i] = this.f_209298_[((verticalCellBlockCount - 1 - cellBlockY) * horizontalCellBlockCount + cellBlockX) * horizontalCellBlockCount + cellBlockZ];
                    } else {
                        return false;
                    }
                }
                return true;
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
        return this.f_209297_;
    }

    @Override
    public DensityFunction harichunk$withDelegate(DensityFunction delegate) {
        return (DensityFunction) (Object) this;
    }
}
