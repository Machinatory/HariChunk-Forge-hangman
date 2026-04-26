package com.hari.harichunk.tfthreadsafetyaddon.mixin;

import com.hari.harichunk.tfthreadsafetyaddon.TFThreadSafetyAddon;
import com.hari.harichunk.tfthreadsafetyaddon.common.ducks.LazyAreaExtension;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import twilightforest.world.components.layer.vanillalegacy.Area;
import twilightforest.world.components.layer.vanillalegacy.area.LazyArea;

@Mixin(value = LazyArea.class, remap = false)
public class MixinLazyArea implements LazyAreaExtension {

    @Shadow @Final private Area transformer;
    @Shadow @Final private int maxCache;
    private Long2ObjectLinkedOpenHashMap<ResourceKey<Biome>> tfthreadsafetyaddon$cachedSamples;

    @Override
    public void tfthreadsafetyaddon$setCachedSamples(Long2ObjectLinkedOpenHashMap<ResourceKey<Biome>> cachedSamples) {
        this.tfthreadsafetyaddon$cachedSamples = cachedSamples;
    }

    @WrapMethod(method = "getBiome")
    private ResourceKey<Biome> wrapSample(int biomeX, int biomeZ, Operation<ResourceKey<Biome>> original) {
        Long2ObjectLinkedOpenHashMap<ResourceKey<Biome>> tfthreadsafetyaddon$cachedSamples1 = this.tfthreadsafetyaddon$cachedSamples;
        if (tfthreadsafetyaddon$cachedSamples1 == null) {
            TFThreadSafetyAddon.LOGGER.warn("LazyArea.tfthreadsafetyaddon$cachedSamples is null", new Throwable());
            return original.call(biomeX, biomeZ);
        }
        long i = ChunkPos.asLong(biomeX, biomeZ);
        ResourceKey<Biome> cached;
        synchronized (tfthreadsafetyaddon$cachedSamples1) {
            cached = tfthreadsafetyaddon$cachedSamples1.getAndMoveToLast(i);
            if (cached != null && cached != Biomes.THE_VOID) {
                return cached;
            }
        }

        ResourceKey<Biome> computed = this.transformer.getBiome(biomeX, biomeZ);
        synchronized (tfthreadsafetyaddon$cachedSamples1) {
            cached = tfthreadsafetyaddon$cachedSamples1.getAndMoveToLast(i);
            if (cached != null && cached != Biomes.THE_VOID) {
                return cached;
            }
            tfthreadsafetyaddon$cachedSamples1.put(i, computed);
            if (tfthreadsafetyaddon$cachedSamples1.size() > this.maxCache) {
                for (int l = 0; l < this.maxCache / 16; ++l) {
                    tfthreadsafetyaddon$cachedSamples1.removeFirst();
                }
            }
        }
        return computed;
    }

}
