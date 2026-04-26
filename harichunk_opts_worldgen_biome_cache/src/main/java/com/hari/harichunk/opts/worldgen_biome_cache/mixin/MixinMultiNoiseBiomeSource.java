package com.hari.harichunk.opts.worldgen_biome_cache.mixin;

import com.hari.harichunk.opts.worldgen_biome_cache.common.BiomeCache;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into MultiNoiseBiomeSource to cache noise biome lookups.
 * This is the primary biome source used in custom world generation,
 * and the most expensive biome computation path.
 */
@Mixin(MultiNoiseBiomeSource.class)
public class MixinMultiNoiseBiomeSource {

    @Inject(method = "getNoiseBiome", at = @At("HEAD"), cancellable = true)
    private void onGetNoiseBiome(int x, int y, int z, Climate.Sampler sampler,
                                  CallbackInfoReturnable<Holder<Biome>> cir) {
        MultiNoiseBiomeSource self = (MultiNoiseBiomeSource) (Object) this;
        Holder<Biome> cached = BiomeCache.getNoiseBiome(self, x, y, z, () -> null);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getNoiseBiome", at = @At("RETURN"))
    private void onGetNoiseBiomeReturn(int x, int y, int z, Climate.Sampler sampler,
                                        CallbackInfoReturnable<Holder<Biome>> cir) {
        if (cir.getReturnValue() != null) {
            MultiNoiseBiomeSource self = (MultiNoiseBiomeSource) (Object) this;
            // Store in cache for future lookups
            BiomeCache.getNoiseBiome(self, x, y, z, () -> cir.getReturnValue());
        }
    }
}
