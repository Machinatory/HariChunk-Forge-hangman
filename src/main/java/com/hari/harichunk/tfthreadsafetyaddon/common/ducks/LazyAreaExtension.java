package com.hari.harichunk.tfthreadsafetyaddon.common.ducks;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public interface LazyAreaExtension {

    void tfthreadsafetyaddon$setCachedSamples(Long2ObjectLinkedOpenHashMap<ResourceKey<Biome>> cachedSamples);

}
