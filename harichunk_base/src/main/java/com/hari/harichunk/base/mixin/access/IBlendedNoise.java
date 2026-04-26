package com.hari.harichunk.base.mixin.access;

import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlendedNoise.class)
public interface IBlendedNoise {

    @Accessor
    PerlinNoise getMinLimitNoise();

    @Accessor
    PerlinNoise getMaxLimitNoise();

    @Accessor
    PerlinNoise getMainNoise();

    @Accessor
    double getXzScale();

    @Accessor
    double getYScale();

    @Accessor
    double getXzMultiplier();

    @Accessor
    double getYMultiplier();

    @Accessor
    double getXzFactor();

    @Accessor
    double getYFactor();

    @Accessor
    double getSmearScaleMultiplier();

    @Accessor
    double getMaxValue();

}
