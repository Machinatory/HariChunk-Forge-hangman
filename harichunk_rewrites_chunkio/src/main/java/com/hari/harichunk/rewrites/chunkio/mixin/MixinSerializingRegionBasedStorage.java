package com.hari.harichunk.rewrites.chunkio.mixin;

import com.hari.harichunk.rewrites.chunkio.common.HariChunkStorageVanillaInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.SectionStorage;

@Mixin(SectionStorage.class)
public class MixinSerializingRegionBasedStorage {

    @Shadow @Final protected LevelHeightAccessor levelHeightAccessor;

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/world/level/chunk/storage/IOWorker"))
    private IOWorker redirectStorageIoWorker(Path directory, boolean dsync, String name) {
        return new HariChunkStorageVanillaInterface(directory, dsync, name);
    }

}
