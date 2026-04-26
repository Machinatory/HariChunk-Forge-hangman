package com.hari.harichunk.opts.dfc.common.gen;

import com.google.common.base.Suppliers;
import com.hari.harichunk.opts.dfc.common.ducks.IBlendingAwareVisitor;
import com.hari.harichunk.opts.dfc.common.ducks.IFastCacheLike;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Supplier;

public class CompiledDensityFunction extends SubCompiledDensityFunction {

    private final CompiledEntry compiledEntry;

    public CompiledDensityFunction(CompiledEntry compiledEntry, DensityFunction blendingFallback) {
        super(compiledEntry, compiledEntry, blendingFallback);
        this.compiledEntry = Objects.requireNonNull(compiledEntry);
    }

    private CompiledDensityFunction(CompiledEntry compiledEntry, Supplier<DensityFunction> blendingFallback) {
        super(compiledEntry, compiledEntry, blendingFallback);
        this.compiledEntry = Objects.requireNonNull(compiledEntry);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        if (visitor instanceof IBlendingAwareVisitor blendingAwareVisitor && blendingAwareVisitor.harichunk$isBlendingEnabled()) {
            DensityFunction fallback1 = this.getFallback();
            if (fallback1 == null) {
                throw new IllegalStateException("blendingFallback is no more");
            }
            return fallback1.mapAll(visitor);
        }
        boolean modified = false;
        List<Object> args = this.compiledEntry.getArgs();
        for (ListIterator<Object> iterator = args.listIterator(); iterator.hasNext(); ) {
            Object next = iterator.next();
            if (next instanceof DensityFunction df) {
                if (!(df instanceof IFastCacheLike)) {
                    DensityFunction applied = df.mapAll(visitor);
                    if (df != applied) {
                        iterator.set(applied);
                        modified = true;
                    }
                }
            }
            if (next instanceof DensityFunction.NoiseHolder noise) {
                // NoiseHolder is immutable, no transformation needed
            }
        }

        for (ListIterator<Object> iterator = args.listIterator(); iterator.hasNext(); ) {
            Object next = iterator.next();
            if (next instanceof IFastCacheLike cacheLike) {
                DensityFunction applied = visitor.apply((DensityFunction) cacheLike);
                if (applied == cacheLike.harichunk$getDelegate()) {
                    iterator.set(null); // cache removed
                    modified = true;
                } else if (applied instanceof IFastCacheLike newCacheLike) {
                    iterator.set(newCacheLike);
                    modified = true;
                } else {
                    throw new UnsupportedOperationException("Unsupported transformation on Marker node");
                }
            }
        }

        Supplier<DensityFunction> fallback = this.blendingFallback != null ? Suppliers.memoize(() -> {
            DensityFunction densityFunction = this.blendingFallback.get();
            return densityFunction != null ? densityFunction.mapAll(visitor) : null;
        }) : null;
        if (fallback != this.blendingFallback) {
            modified = true;
        }
        if (modified) {
            return new CompiledDensityFunction(this.compiledEntry.newInstance(args), fallback);
        } else {
            return this;
        }
    }

}
