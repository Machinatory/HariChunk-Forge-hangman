package com.hari.harichunk.tfthreadsafetyaddon.mixin;

import com.hari.harichunk.tfthreadsafetyaddon.common.ThreadLocalMazeRandom;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import twilightforest.world.components.structures.minotaurmaze.MinotaurMazeComponent;

@Mixin(value = MinotaurMazeComponent.class, remap = false)
public class MixinMinotaurMazeComponent {

    @WrapMethod(method = "m_213694_")
    private void tfthreadsafetyaddon$wrapPostProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox boundingBox,
            ChunkPos chunkPos,
            BlockPos blockPos,
            Operation<Void> original
    ) {
        ThreadLocalMazeRandom.set(random);
        try {
            original.call(level, structureManager, chunkGenerator, random, boundingBox, chunkPos, blockPos);
        } finally {
            ThreadLocalMazeRandom.clear();
        }
    }
}
