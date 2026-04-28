package com.hari.harichunk.opts.scheduling.mixin.idle_tasks.autosave.enhanced_autosave;

import com.hari.harichunk.opts.scheduling.common.Config;
import com.hari.harichunk.opts.scheduling.common.idle_tasks.IThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage implements IThreadedAnvilChunkStorage {

    @Shadow(remap = false) @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> f_140129_; // updatingChunkMap

    @Shadow protected abstract boolean saveChunkIfNeeded(ChunkHolder chunkHolder);

    @Unique
    private final Object2LongLinkedOpenHashMap<ChunkPos> dirtyChunkPosForAutoSave = new Object2LongLinkedOpenHashMap<>();

    @Override
    public void enqueueDirtyChunkPosForAutoSave(ChunkPos chunkPos) {
        if (chunkPos == null) {
            return;
        }
        synchronized (this.dirtyChunkPosForAutoSave) {
            this.dirtyChunkPosForAutoSave.putAndMoveToLast(chunkPos, System.currentTimeMillis());
        }
    }

    @Override
    public boolean runOneChunkAutoSave() {
        synchronized (this.dirtyChunkPosForAutoSave) {
            final ObjectBidirectionalIterator<Object2LongMap.Entry<ChunkPos>> iterator = this.dirtyChunkPosForAutoSave.object2LongEntrySet().fastIterator();
            while (iterator.hasNext()) {
                final Object2LongMap.Entry<ChunkPos> entry = iterator.next();
                if (System.currentTimeMillis() - entry.getLongValue() < Config.autoSaveDelayMillis) break;
                iterator.remove();
                if (entry.getKey() == null) continue;
                ChunkHolder chunkHolder = this.f_140129_.get(entry.getKey().toLong());
                if (chunkHolder == null) continue;
                final CompletableFuture<ChunkAccess> savingFuture = chunkHolder.getChunkToSave();
                if (savingFuture.isDone()) {
                    this.saveChunkIfNeeded(chunkHolder);
                    return true;
                } else {
                    savingFuture.handle((chunk, throwable) -> {
                        this.enqueueDirtyChunkPosForAutoSave(chunkHolder.getPos());
                        return null;
                    });
                }
            }
        }

        return false;
    }
}
