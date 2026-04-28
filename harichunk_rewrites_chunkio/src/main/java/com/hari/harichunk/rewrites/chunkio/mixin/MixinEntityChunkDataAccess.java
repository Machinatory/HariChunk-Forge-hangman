package com.hari.harichunk.rewrites.chunkio.mixin;

import com.hari.harichunk.rewrites.chunkio.common.HariChunkStorageVanillaInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;

@Mixin(EntityStorage.class)
public class MixinEntityChunkDataAccess {

    @Shadow(remap = false)
    @Final
    private ServerLevel f_156538_;  // level

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/world/level/chunk/storage/IOWorker"))
    private IOWorker redirectStorageIoWorker(Path directory, boolean dsync, String name) {
        return new HariChunkStorageVanillaInterface(directory, dsync, name);
    }

}
