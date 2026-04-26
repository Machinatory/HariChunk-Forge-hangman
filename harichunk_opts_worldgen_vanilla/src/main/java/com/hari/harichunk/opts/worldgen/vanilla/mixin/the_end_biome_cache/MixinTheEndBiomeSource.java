package com.hari.harichunk.opts.worldgen.vanilla.mixin.the_end_biome_cache;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TheEndBiomeSource.class)
public abstract class MixinTheEndBiomeSource {

    @Shadow @Final private Holder<Biome> highlands;

    @Shadow @Final private Holder<Biome> midlands;

    @Shadow @Final private Holder<Biome> islands;

    @Shadow @Final private Holder<Biome> barrens;

    @Shadow @Final private Holder<Biome> end;

    private Holder<Biome> getBiomeForNoiseGenVanilla(int x, int y, int z, Climate.Sampler noise) {
        // TODO [VanillaCopy]
        int i = QuartPos.toBlock(x);
        int j = QuartPos.toBlock(y);
        int k = QuartPos.toBlock(z);
        int l = SectionPos.blockToSectionCoord(i);
        int m = SectionPos.blockToSectionCoord(k);
        if ((long)l * (long)l + (long)m * (long)m <= 4096L) {
            return this.end;
        } else {
            int n = (SectionPos.blockToSectionCoord(i) * 2 + 1) * 8;
            int o = (SectionPos.blockToSectionCoord(k) * 2 + 1) * 8;
            double d = noise.erosion().compute(new DensityFunction.SinglePointContext(n, j, o));
            if (d > 0.25D) {
                return this.highlands;
            } else if (d >= -0.0625D) {
                return this.midlands;
            } else {
                return d < -0.21875D ? this.islands : this.barrens;
            }
        }
    }

    private final ThreadLocal<Long2ObjectLinkedOpenHashMap<Holder<Biome>>> cache = ThreadLocal.withInitial(Long2ObjectLinkedOpenHashMap::new);
    private final int cacheCapacity = 1024;

    /**
     * @author Hari
     * @reason the end biome cache
     */
    @Overwrite
    public Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ, Climate.Sampler multiNoiseSampler) {
        final long key = ChunkPos.asLong(biomeX, biomeZ);
        final Long2ObjectLinkedOpenHashMap<Holder<Biome>> cacheThreadLocal = cache.get();
        final Holder<Biome> biome = cacheThreadLocal.get(key);
        if (biome != null) {
            return biome;
        } else {
            final Holder<Biome> gennedBiome = getBiomeForNoiseGenVanilla(biomeX, biomeY, biomeZ, multiNoiseSampler);
            cacheThreadLocal.put(key, gennedBiome);
            if (cacheThreadLocal.size() > cacheCapacity) {
                for (int i = 0; i < cacheCapacity / 16; i ++) {
                    cacheThreadLocal.removeFirst();
                }
            }
            return gennedBiome;
        }
    }

}
