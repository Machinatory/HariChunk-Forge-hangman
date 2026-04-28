package com.hari.harichunk.rewrites.chunkio.mixin;

import com.hari.harichunk.rewrites.chunkio.common.HariChunkStorageVanillaInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;

@Mixin(IOWorker.class)
public class MixinStorageIoWorker {

    @Mutable
    @Shadow(remap = false) @Final private Map<ChunkPos, IOWorker.PendingStore> f_63519_;  // pendingWrites

    @Mutable
    @Shadow(remap = false) @Final private RegionFileStorage f_63518_;  // storage

    @Mutable
    @Shadow(remap = false) @Final private AtomicBoolean f_63516_;  // shutdownRequested

    @Mutable
    @Shadow(remap = false) @Final private ProcessorMailbox<StrictQueue.IntRunnable> f_63517_;  // mailbox

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if ((Object) this instanceof HariChunkStorageVanillaInterface) {
            // fail-fast incompatibility
            this.f_63519_ = null;
            this.f_63518_ = null;
            this.f_63516_ = null;
            this.f_63517_ = null;
        }
    }

}
