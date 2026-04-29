package com.hari.harichunk.threading.chunkio.mixin;

import com.hari.harichunk.threading.chunkio.common.ChunkIoThreadingExecutorUtils;
import com.hari.harichunk.threading.chunkio.common.IAsyncChunkStorage;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;

@Mixin(IOWorker.class)
public abstract class MixinStorageIoWorker implements IAsyncChunkStorage {

    @Shadow(remap = false) public abstract CompletableFuture<Optional<CompoundTag>> m_156587_(ChunkPos pos);  // loadAsync

    @Shadow(remap = false) protected abstract boolean m_223484_(CompoundTag nbt);  // isOldChunk

    @Shadow(remap = false) public abstract CompletableFuture<Void> m_196358_(ChunkPos pos, StreamTagVisitor scanner);  // scanChunk

    @Shadow(remap = false) @Final private static Logger f_63515_;  // LOGGER
    private ExecutorService threadExecutor;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;ioPool()Ljava/util/concurrent/ExecutorService;"))
    private ExecutorService redirectIoWorkerExecutor() {
        return threadExecutor = Executors.newSingleThreadExecutor(ChunkIoThreadingExecutorUtils.ioWorkerFactory);
    }

    @Override
    public CompletableFuture<Optional<CompoundTag>> getNbtAtAsync(ChunkPos pos) {
        return m_156587_(pos);
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ProcessorMailbox;close()V", shift = At.Shift.AFTER))
    private void onClose(CallbackInfo ci) {
        threadExecutor.shutdown();
        while (!threadExecutor.isTerminated()) {
            LockSupport.parkNanos("Waiting for thread executor termination", 100_000);
        }
    }

    /**
     * @author Hari
     * @reason use async instead of flooding worker thread
     */
    @Overwrite(remap = false)
    private CompletableFuture<BitSet> m_223489_(int chunkX, int chunkZ) {
        ChunkPos chunkPos = ChunkPos.minFromRegion(chunkX, chunkZ);
        ChunkPos chunkPos2 = ChunkPos.maxFromRegion(chunkX, chunkZ);
        BitSet bitSet = new BitSet();
        final CompletableFuture[] futures = ChunkPos.rangeClosed(chunkPos, chunkPos2)
                .map(chunkPosx -> {
                    CollectFields selectiveNbtCollector = new CollectFields(new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector(CompoundTag.TYPE, "blending_data"));

                    return this.m_196358_(chunkPosx, selectiveNbtCollector)
                            .thenRun(() -> {
                                Tag nbtElement = selectiveNbtCollector.getResult();
                                if (nbtElement instanceof CompoundTag nbtCompound) {
                                    if (this.m_223484_(nbtCompound)) {
                                        int i = chunkPosx.getRegionLocalZ() * 32 + chunkPosx.getRegionLocalX();
                                        synchronized (bitSet) {
                                            bitSet.set(i);
                                        }
                                    }
                                }
                            })
                            .exceptionally(throwable -> {
                                f_63515_.warn("Failed to scan chunk {}", chunkPosx, throwable);
                                return null;
                            });
                }).toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
                .thenApply(unused -> bitSet);
    }
}
