package com.hari.harichunk.opts.chunkio.mixin.limit_nbt_cache;

import com.hari.harichunk.opts.chunkio.common.Config;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Map;

@Mixin(value = IOWorker.class, priority = 990)
public abstract class MixinStorageIoWorker {

    @Shadow(remap = false) @Final private Map<ChunkPos, IOWorker.PendingStore> f_63519_;  // pendingWrites

    @Shadow(remap = false) protected abstract void m_63535_(ChunkPos pos, IOWorker.PendingStore result);  // runStore

    @Shadow(remap = false) protected abstract void m_63561_();  // tellStorePending

    @Shadow(remap = false) @Final private static Logger f_63515_;  // LOGGER

    @Dynamic
    @Inject(method = "m_223468_", at = @At("HEAD"))
    private void preTask(CallbackInfo ci) {
        checkHardLimit();
    }

    @Inject(method = "storePendingChunk", at = @At("HEAD"))
    private void onWriteResult(CallbackInfo ci) {
        if (!this.f_63519_.isEmpty()) {
            checkHardLimit();
            if (this.f_63519_.size() >= Config.chunkDataCacheSoftLimit) {
                int writeFrequency = Math.min(1, (this.f_63519_.size() - (int) Config.chunkDataCacheSoftLimit) / 16);
                for (int i = 0; i < writeFrequency; i++) {
                    writeResult0();
                }
            }
        }
    }

    @Unique
    private void checkHardLimit() {
        if (this.f_63519_.size() >= Config.chunkDataCacheLimit) {
            LOGGER.warn("Chunk data cache size exceeded hard limit ({} >= {}), forcing writes to disk (you can increase chunkDataCacheLimit in harichunk.toml)", this.f_63519_.size(), Config.chunkDataCacheLimit);
            while (this.f_63519_.size() >= Config.chunkDataCacheSoftLimit * 0.75) { // using chunkDataCacheSoftLimit is intentional
                writeResult0();
            }
        }
    }

    @Unique
    private void writeResult0() {
        // TODO [VanillaCopy] writeResult
        Iterator<Map.Entry<ChunkPos, IOWorker.PendingStore>> iterator = this.f_63519_.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<ChunkPos, IOWorker.PendingStore> entry = iterator.next();
            iterator.remove();
            this.m_63535_(entry.getKey(), entry.getValue());
        }
    }

}
