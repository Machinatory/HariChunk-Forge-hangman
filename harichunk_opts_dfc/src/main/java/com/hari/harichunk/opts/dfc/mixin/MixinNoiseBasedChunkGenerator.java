package com.hari.harichunk.opts.dfc.mixin;

import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Placeholder mixin for NoiseBasedChunkGenerator.
 *
 * In MC 1.20.1, there is no NoiseConfig class (unlike 1.21.x).
 * The density function compilation is handled in MixinNoiseChunk via
 * visitor wrapping during NoiseChunk construction.
 *
 * This mixin exists in the config for compatibility but the actual
 * compilation work is delegated to the MixinNoiseChunk which intercepts
 * the NoiseRouter.mapAll() calls during NoiseChunk initialization.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class MixinNoiseBasedChunkGenerator {
    // Compilation is handled in MixinNoiseChunk
}
