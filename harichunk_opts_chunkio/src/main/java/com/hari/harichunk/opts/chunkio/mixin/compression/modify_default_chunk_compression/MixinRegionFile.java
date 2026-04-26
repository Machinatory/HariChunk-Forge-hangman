package com.hari.harichunk.opts.chunkio.mixin.compression.modify_default_chunk_compression;

import com.hari.harichunk.opts.chunkio.common.ConfigConstants;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RegionFile.class)
public class MixinRegionFile {

    @Redirect(method = "<init>(Ljava/nio/file/Path;Ljava/nio/file/Path;Z)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/storage/RegionFileVersion;VERSION_DEFLATE:Lnet/minecraft/world/level/chunk/storage/RegionFileVersion;", opcode = Opcodes.GETSTATIC))
    private static RegionFileVersion redirectDefaultChunkStreamVersion() {
        return ConfigConstants.CHUNK_STREAM_VERSION;
    }

}
