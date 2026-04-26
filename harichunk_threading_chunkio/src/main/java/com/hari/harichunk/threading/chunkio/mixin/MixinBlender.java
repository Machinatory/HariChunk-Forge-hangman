package com.hari.harichunk.threading.chunkio.mixin;

import com.hari.harichunk.threading.chunkio.common.ProtoChunkExtension;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Blender.class)
public class MixinBlender {

    @Redirect(method = "of", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/WorldGenRegion;isOldChunkAround(Lnet/minecraft/world/level/ChunkPos;I)Z"))
    private static boolean redirectNeedsBlending(WorldGenRegion instance, ChunkPos chunkPos, int checkRadius) {
        return ((ProtoChunkExtension) instance.getChunk(chunkPos.x, chunkPos.z)).getNeedBlending();
    }

}
