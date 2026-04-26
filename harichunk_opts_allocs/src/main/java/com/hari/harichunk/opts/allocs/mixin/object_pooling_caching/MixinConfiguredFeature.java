package com.hari.harichunk.opts.allocs.mixin.object_pooling_caching;

import com.hari.harichunk.opts.allocs.common.PooledFeatureContext;
import com.hari.harichunk.base.common.structs.SimpleObjectPool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

@Mixin(ConfiguredFeature.class)
public class MixinConfiguredFeature<FC extends FeatureConfiguration, F extends Feature<FC>> {

    @Shadow @Final public F feature;

    @Shadow @Final public FC config;

    /**
     * @author Hari
     * @reason pool FeatureContext
     */
    @Overwrite
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (!world.ensureCanWrite(origin)) return false;
        final SimpleObjectPool<PooledFeatureContext<?>> pool = PooledFeatureContext.POOL.get();
        final PooledFeatureContext<FC> context = (PooledFeatureContext<FC>) pool.alloc();
        try {
            context.reInit(Optional.empty(), world, chunkGenerator, random, origin, this.config);
            return this.feature.place(context);
        } finally {
            context.reInit();
            pool.release(context);
        }
    }

}
