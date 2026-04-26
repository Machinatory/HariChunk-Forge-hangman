package com.hari.harichunk.opts.worldgen_biome_cache.mixin;

import com.hari.harichunk.opts.worldgen_biome_cache.common.BiomeCache;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into FixedBiomeSource - always returns the same biome, so caching
 * is trivial but this ensures the cache path is exercised for all biome sources.
 */
@Mixin(FixedBiomeSource.class)
public class MixinFixedBiomeSource {

    @Shadow @Final private Holder<Biome> biome;

    @Inject(method = "getNoiseBiome", at = @At("HEAD"), cancellable = true)
    private void onGetNoiseBiome(int x, int y, int z, Climate.Sampler sampler,
                                  CallbackInfoReturnable<Holder<Biome>> cir) {
        // FixedBiomeSource always returns the same biome, short-circuit directly
        cir.setReturnValue(biome);
    }
}
