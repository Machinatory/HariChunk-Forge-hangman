package com.hari.harichunk.opts.worldgen.vanilla.mixin.aquifer;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NoiseBasedChunkGenerator.class)
public class MixinNoiseChunkGenerator {

    /**
     * @author Hari
     * @reason optimize
     */
    @Overwrite
    private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings chunkGeneratorSettings) {
        Aquifer.FluidStatus fluidLevel = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int i = chunkGeneratorSettings.seaLevel();
        Aquifer.FluidStatus fluidLevel2 = new Aquifer.FluidStatus(i, chunkGeneratorSettings.defaultFluid());
        final int min = Math.min(-54, i);
        final Aquifer.FluidPicker sampler = (j, k, lx) -> k < min ? fluidLevel : fluidLevel2;
        return sampler;
    }

}
