package com.hari.harichunk.opts.dfc.common.gen;

import com.google.common.base.Suppliers;
import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.ducks.IArrayCacheCapable;
import com.hari.harichunk.opts.dfc.common.ducks.IBlendingAwareVisitor;
import com.hari.harichunk.opts.dfc.common.ducks.ICoordinatesFilling;
import com.hari.harichunk.opts.dfc.common.util.ArrayCache;
import com.hari.harichunk.opts.dfc.common.vif.ContextProviderVanillaInterface;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

public class SubCompiledDensityFunction implements DensityFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubCompiledDensityFunction.class);

    private final ISingleMethod singleMethod;
    private final IMultiMethod multiMethod;
    protected final Supplier<DensityFunction> blendingFallback;

    // also called from generated code
    public SubCompiledDensityFunction(ISingleMethod singleMethod, IMultiMethod multiMethod, DensityFunction blendingFallback) {
        this(singleMethod, multiMethod, unwrap(blendingFallback));
    }

    protected SubCompiledDensityFunction(ISingleMethod singleMethod, IMultiMethod multiMethod, Supplier<DensityFunction> blendingFallback) {
        this.singleMethod = Objects.requireNonNull(singleMethod);
        this.multiMethod = Objects.requireNonNull(multiMethod);
        this.blendingFallback = blendingFallback;
    }

    private static Supplier<DensityFunction> unwrap(DensityFunction densityFunction) {
        if (densityFunction instanceof SubCompiledDensityFunction scdf) {
            return scdf.blendingFallback;
        } else {
            return densityFunction != null ? Suppliers.ofInstance(densityFunction) : null;
        }
    }

    @Override
    public double compute(FunctionContext context) {
        return this.singleMethod.evalSingle(context.blockX(), context.blockY(), context.blockZ(), EvalType.fromContext(context));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        if (provider instanceof ContextProviderVanillaInterface vanillaInterface) {
            this.multiMethod.evalMulti(densities, vanillaInterface.getX(), vanillaInterface.getY(), vanillaInterface.getZ(), EvalType.fromContext(provider), vanillaInterface.harichunk$getArrayCache());
            return;
        }

        ArrayCache cache = provider instanceof IArrayCacheCapable cacheCapable ? cacheCapable.harichunk$getArrayCache() : new ArrayCache();
        int[] x = cache.getIntArray(densities.length, false);
        int[] y = cache.getIntArray(densities.length, false);
        int[] z = cache.getIntArray(densities.length, false);
        if (provider instanceof ICoordinatesFilling coordinatesFilling) {
            coordinatesFilling.harichunk$fillCoordinates(x, y, z);
        } else {
            for (int i = 0; i < densities.length; i++) {
                FunctionContext pos = provider.forIndex(i);
                x[i] = pos.blockX();
                y[i] = pos.blockY();
                z[i] = pos.blockZ();
            }
        }
        this.multiMethod.evalMulti(densities, x, y, z, EvalType.fromContext(provider), cache);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        if (this.getClass() != SubCompiledDensityFunction.class) {
            throw new AbstractMethodError();
        }
        if (visitor instanceof IBlendingAwareVisitor blendingAwareVisitor && blendingAwareVisitor.harichunk$isBlendingEnabled()) {
            DensityFunction fallback1 = this.getFallback();
            if (fallback1 == null) {
                throw new IllegalStateException("blendingFallback is no more");
            }
            return fallback1.mapAll(visitor);
        }
        boolean modified = false;
        Supplier<DensityFunction> fallback = this.blendingFallback != null ? Suppliers.memoize(() -> {
            DensityFunction densityFunction = this.blendingFallback.get();
            return densityFunction != null ? densityFunction.mapAll(visitor) : null;
        }) : null;
        if (fallback != this.blendingFallback) {
            modified = true;
        }
        if (modified) {
            return new SubCompiledDensityFunction(this.singleMethod, this.multiMethod, fallback);
        } else {
            return this;
        }
    }

    @Override
    public double minValue() {
//        DensityFunction fallback = this.getFallback();
//        return fallback != null ? fallback.minValue() : Double.MIN_VALUE;
        return Double.MIN_VALUE;
    }

    @Override
    public double maxValue() {
//        DensityFunction fallback = this.getFallback();
//        return fallback != null ? fallback.maxValue() : Double.MAX_VALUE;
        return Double.MAX_VALUE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        throw new UnsupportedOperationException("SubCompiledDensityFunction does not support codec serialization");
    }

    protected DensityFunction getFallback() {
        return this.blendingFallback != null ? this.blendingFallback.get() : null;
    }
}
