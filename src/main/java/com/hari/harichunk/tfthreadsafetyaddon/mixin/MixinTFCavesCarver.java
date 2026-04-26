package com.hari.harichunk.tfthreadsafetyaddon.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import twilightforest.world.components.TFCavesCarver;

import java.util.function.Function;

@Mixin(value = TFCavesCarver.class, remap = false)
public class MixinTFCavesCarver {

    @Unique
    private static final ThreadLocal<RandomSource> tfthreadsafetyaddon$carverRandom = new ThreadLocal<>();

    @WrapMethod(method = "carve")
    private boolean tfthreadsafetyaddon$wrapCarve(
            CarvingContext context,
            CaveCarverConfiguration config,
            ChunkAccess chunk,
            Function<BlockPos, Holder<Biome>> biomeGetter,
            RandomSource random,
            Aquifer aquifer,
            ChunkPos chunkPos,
            CarvingMask carvingMask,
            Operation<Boolean> original
    ) {
        tfthreadsafetyaddon$carverRandom.set(random);
        try {
            return original.call(context, config, chunk, biomeGetter, random, aquifer, chunkPos, carvingMask);
        } finally {
            tfthreadsafetyaddon$carverRandom.remove();
        }
    }

    @WrapOperation(
            method = "carveBlock",
            at = @At(value = "FIELD", target = "Ltwilightforest/world/components/TFCavesCarver;rand:Lnet/minecraft/util/RandomSource;")
    )
    private RandomSource tfthreadsafetyaddon$useThreadLocalRandom(TFCavesCarver instance, Operation<RandomSource> original) {
        RandomSource random = tfthreadsafetyaddon$carverRandom.get();
        return random != null ? random : original.call(instance);
    }
}
