package com.hari.harichunk.fixes.chunkio.threading_issues.mixin;

import com.hari.harichunk.base.mixin.access.IThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;

@Mixin(ChunkHolder.class)
public class MixinChunkHolder {

    @Inject(method = "updateFutures", at = @At("HEAD"))
    private void beforeTick(ChunkMap chunkStorage, Executor executor, CallbackInfo ci) {
        ((IThreadedAnvilChunkStorage) chunkStorage).invokeUpdateHolderMap();
    }

}
