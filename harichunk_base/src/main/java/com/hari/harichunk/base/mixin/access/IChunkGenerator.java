package com.hari.harichunk.base.mixin.access;

import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkGenerator.class)
public interface IChunkGenerator {

//    @Invoker
//    void invokeGenerateStrongholdPositions();

}
