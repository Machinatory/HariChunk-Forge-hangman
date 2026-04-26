package com.hari.harichunk.opts.dfc.common.ast.dfvisitor;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;

public class StripBlending implements DensityFunction.Visitor {

    public static final StripBlending INSTANCE = new StripBlending();

    private StripBlending() {
    }

    @Override
    public DensityFunction apply(DensityFunction densityFunction) {
        if (densityFunction instanceof NoiseChunk.BlendAlpha) {
            return DensityFunctions.constant(1.0);
        } else if (densityFunction instanceof NoiseChunk.BlendOffset) {
            return DensityFunctions.constant(0.0);
        } else if (densityFunction instanceof DensityFunctions.BlendAlpha) {
            return DensityFunctions.constant(1.0);
        } else if (densityFunction instanceof DensityFunctions.BlendOffset) {
            return DensityFunctions.constant(0.0);
        } else {
            return densityFunction;
        }
    }

    @Override
    public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
        return noise;
    }

}
