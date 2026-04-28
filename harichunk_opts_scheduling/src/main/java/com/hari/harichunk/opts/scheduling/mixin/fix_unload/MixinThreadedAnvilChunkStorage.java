package com.hari.harichunk.opts.scheduling.mixin.fix_unload;

import com.hari.harichunk.base.common.structs.LongHashSet;
import com.hari.harichunk.base.common.util.ShouldKeepTickingUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.thread.BlockableEventLoop;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow(remap = false) @Final private BlockableEventLoop<Runnable> f_140135_; // mainThreadExecutor

    @Shadow(remap = false) protected abstract void m_140353_(BooleanSupplier shouldKeepTicking); // processUnloads

    @Mutable
    @Shadow(remap = false) @Final private LongSet f_140139_; // toDrop

    @ModifyArg(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;tick(Ljava/util/function/BooleanSupplier;)V"))
    private BooleanSupplier redirectTickPointOfInterestStorageTick(BooleanSupplier shouldKeepTicking) {
        return ShouldKeepTickingUtils.minimumTicks(shouldKeepTicking, 32);
    }

    @ModifyArg(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;processUnloads(Ljava/util/function/BooleanSupplier;)V"))
    private BooleanSupplier redirectTickUnloadChunks(BooleanSupplier shouldKeepTicking) {
        return () -> true;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.f_140139_ = new LongHashSet();
    }

}
