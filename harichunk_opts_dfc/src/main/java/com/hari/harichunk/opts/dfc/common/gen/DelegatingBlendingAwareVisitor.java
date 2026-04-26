package com.hari.harichunk.opts.dfc.common.gen;

import com.hari.harichunk.opts.dfc.common.ducks.IBlendingAwareVisitor;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Objects;

public class DelegatingBlendingAwareVisitor implements IBlendingAwareVisitor, DensityFunction.Visitor {

    private final DensityFunction.Visitor delegate;
    private final boolean blendingEnabled;

    public DelegatingBlendingAwareVisitor(DensityFunction.Visitor delegate, boolean blendingEnabled) {
        this.delegate = Objects.requireNonNull(delegate);
        this.blendingEnabled = blendingEnabled;
    }

    @Override
    public DensityFunction apply(DensityFunction densityFunction) {
        return this.delegate.apply(densityFunction);
    }

    @Override
    public boolean harichunk$isBlendingEnabled() {
        return this.blendingEnabled;
    }
}
