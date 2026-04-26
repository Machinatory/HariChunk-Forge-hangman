package com.hari.harichunk.opts.chunkio.mixin.hide_sync_disk_writes_behind_flag;

import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegionFileStorage.class)
public class MixinRegionBasedStorage {

    @Mutable
    @Shadow @Final private boolean sync;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onPostInit(CallbackInfo info) {
        this.sync = Boolean.parseBoolean(System.getProperty("com.hari.harichunk.chunkio.syncDiskWrites", "false"));
    }

}
