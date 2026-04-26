package com.hari.harichunk.tfthreadsafetyaddon.mixin;

import com.hari.harichunk.tfthreadsafetyaddon.common.ThreadLocalMazeRandom;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import twilightforest.world.components.structures.TFMaze;
import twilightforest.world.components.structures.TFStructureComponentOld;

@Mixin(value = TFMaze.class, remap = false)
public class MixinTFMaze {

    @WrapMethod(method = "copyToStructure")
    private void tfthreadsafetyaddon$wrapCopyToStructure(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            int x,
            int y,
            int z,
            TFStructureComponentOld component,
            BoundingBox boundingBox,
            Operation<Void> original
    ) {
        synchronized (this) {
            original.call(level, structureManager, chunkGenerator, x, y, z, component, boundingBox);
        }
    }

    @WrapMethod(method = "shouldTorch")
    private boolean tfthreadsafetyaddon$wrapShouldTorch(int x, int z, Operation<Boolean> original) {
        synchronized (this) {
            return original.call(x, z);
        }
    }

    @WrapMethod(method = "shouldTree")
    private boolean tfthreadsafetyaddon$wrapShouldTree(int x, int z, Operation<Boolean> original) {
        synchronized (this) {
            return original.call(x, z);
        }
    }

    @WrapMethod(method = "rbGen")
    private void tfthreadsafetyaddon$wrapRbGen(int x, int z, Operation<Void> original) {
        synchronized (this) {
            original.call(x, z);
        }
    }

    @WrapMethod(method = "setSeed")
    private void tfthreadsafetyaddon$wrapSetSeed(long seed, Operation<Void> original) {
        synchronized (this) {
            original.call(seed);
        }
    }

    @WrapOperation(
            method = {
                    "shouldTorch",
                    "shouldTree",
                    "rbGen",
                    "setSeed"
            },
            at = @At(value = "FIELD", target = "Ltwilightforest/world/components/structures/TFMaze;rand:Lnet/minecraft/util/RandomSource;")
    )
    private RandomSource tfthreadsafetyaddon$useThreadLocalRandom(TFMaze instance, Operation<RandomSource> original) {
        RandomSource random = ThreadLocalMazeRandom.get();
        if (random != null) {
            return random;
        }
        return original.call(instance);
    }
}
