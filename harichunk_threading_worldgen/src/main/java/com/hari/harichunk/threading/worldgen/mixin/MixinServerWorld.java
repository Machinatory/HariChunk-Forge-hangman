package com.hari.harichunk.threading.worldgen.mixin;

import com.ibm.asyncutil.locks.AsyncLock;
import com.ibm.asyncutil.locks.AsyncNamedLock;
import com.hari.harichunk.threading.worldgen.common.IWorldGenLockable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class MixinServerWorld implements IWorldGenLockable {

    private volatile AsyncLock worldGenSingleThreadedLock = null;
    private volatile AsyncNamedLock<ChunkPos> worldGenChunkLock = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initWorldGenSingleThreadedLock(CallbackInfo ci) {
        worldGenSingleThreadedLock = AsyncLock.createFair();
        worldGenChunkLock = AsyncNamedLock.createFair();
    }

    @Override
    public AsyncLock getWorldGenSingleThreadedLock() {
        return worldGenSingleThreadedLock;
    }

    @Override
    public AsyncNamedLock<ChunkPos> getWorldGenChunkLock() {
        return worldGenChunkLock;
    }
}
