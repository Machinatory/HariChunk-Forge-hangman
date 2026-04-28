package com.hari.harichunk.opts.scheduling.mixin.idle_tasks.autosave.enhanced_autosave;

import com.hari.harichunk.opts.scheduling.common.idle_tasks.IThreadedAnvilChunkStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkAccess.class)
public abstract class MixinChunk {

    @Shadow(remap = false) protected volatile boolean f_62662_; // unsaved

    @Shadow(remap = false) @Final private ChunkPos f_62661_; // pos

    @Inject(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;unsaved:Z", shift = At.Shift.AFTER))
    private void onSetShouldSave(CallbackInfo ci) {
        //noinspection ConstantConditions
        if (this.f_62662_ && (Object) this instanceof LevelChunk worldChunk) {
            if (worldChunk.getLevel() instanceof ServerLevel serverWorld) {
                ((IThreadedAnvilChunkStorage) serverWorld.getChunkSource().chunkMap).enqueueDirtyChunkPosForAutoSave(this.f_62661_);
            }
        }
    }

}
