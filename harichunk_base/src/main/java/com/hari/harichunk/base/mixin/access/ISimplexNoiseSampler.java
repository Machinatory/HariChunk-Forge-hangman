package com.hari.harichunk.base.mixin.access;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimplexNoise.class)
public interface ISimplexNoiseSampler {

    @Accessor
    int[] getP();

}
