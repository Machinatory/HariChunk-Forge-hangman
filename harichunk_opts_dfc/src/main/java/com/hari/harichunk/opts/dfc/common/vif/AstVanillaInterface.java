package com.hari.harichunk.opts.dfc.common.vif;

import com.hari.harichunk.opts.dfc.common.ast.AstNode;
import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.ast.misc.CacheLikeNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.DelegateNode;
import com.hari.harichunk.opts.dfc.common.ducks.IFastCacheLike;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;

import java.util.Objects;

public class AstVanillaInterface implements DensityFunction {

    private final AstNode astNode;
    private final DensityFunction blendingFallback;

    public AstVanillaInterface(AstNode astNode, DensityFunction blendingFallback) {
        this.astNode = Objects.requireNonNull(astNode);
        this.blendingFallback = blendingFallback;
    }

    @Override
    public double compute(FunctionContext context) {
        DensityFunction fallback = this.getBlendingFallback();
        if (fallback != null) {
            return fallback.compute(context);
        } else {
            return this.astNode.evalSingle(context.blockX(), context.blockY(), context.blockZ(), EvalType.fromContext(context));
        }
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        if (provider instanceof NoiseChunk noiseChunk) {
            DensityFunction fallback = this.getBlendingFallback();
            if (fallback != null) {
                fallback.fillArray(densities, provider);
                return;
            }
        }
        if (provider instanceof ContextProviderVanillaInterface vanillaInterface) {
            this.astNode.evalMulti(densities, vanillaInterface.getX(), vanillaInterface.getY(), vanillaInterface.getZ(), EvalType.fromContext(provider));
            return;
        }

        int[] x = new int[densities.length];
        int[] y = new int[densities.length];
        int[] z = new int[densities.length];
        for (int i = 0; i < densities.length; i++) {
            FunctionContext pos = provider.forIndex(i);
            x[i] = pos.blockX();
            y[i] = pos.blockY();
            z[i] = pos.blockZ();
        }
        this.astNode.evalMulti(densities, x, y, z, EvalType.fromContext(provider));
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        AstNode transformed = this.astNode.transform(astNode -> {
            if (astNode instanceof DelegateNode delegateNode) {
                return new DelegateNode(delegateNode.getDelegate().mapAll(visitor));
            }
            if (astNode instanceof CacheLikeNode cacheLikeNode) {
                DensityFunction newDelegate = ((DensityFunction) (Object) cacheLikeNode.getCacheLike()).mapAll(visitor);
                return new CacheLikeNode((IFastCacheLike) newDelegate, cacheLikeNode.getDelegate());
            }
            return astNode;
        });
        DensityFunction blendingFallback1 = this.blendingFallback != null ? this.blendingFallback.mapAll(visitor) : null;
        if (transformed == this.astNode && blendingFallback1 == this.blendingFallback) {
            return this;
        } else {
            return new AstVanillaInterface(
                    transformed,
                    blendingFallback1
            );
        }
    }

    @Override
    public double minValue() {
        return this.blendingFallback.minValue();
    }

    @Override
    public double maxValue() {
        return this.blendingFallback.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        throw new UnsupportedOperationException("AstVanillaInterface does not support codec serialization");
    }

    public AstNode getAstNode() {
        return astNode;
    }

    public DensityFunction getBlendingFallback() {
        return blendingFallback;
    }
}
