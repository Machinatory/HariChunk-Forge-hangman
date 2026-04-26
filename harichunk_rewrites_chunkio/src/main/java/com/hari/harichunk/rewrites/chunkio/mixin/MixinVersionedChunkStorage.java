package com.hari.harichunk.rewrites.chunkio.mixin;

import com.hari.harichunk.rewrites.chunkio.common.HariChunkStorageVanillaInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;

@Mixin(ChunkStorage.class)
public class MixinVersionedChunkStorage {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/world/level/chunk/storage/IOWorker"))
    private IOWorker redirectStorageIoWorker(Path directory, boolean dsync, String name) {
        return new HariChunkStorageVanillaInterface(directory, dsync, name);
    }

}
