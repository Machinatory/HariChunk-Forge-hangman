package com.hari.harichunk.base.mixin.access;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PerlinNoise.class)
public interface IOctavePerlinNoiseSampler {

    @Accessor
    ImprovedNoise[] getNoiseLevels();

    @Accessor
    DoubleList getAmplitudes();

    @Accessor
    double getLowestFreqInputFactor();

    @Accessor
    double getLowestFreqValueFactor();

}
