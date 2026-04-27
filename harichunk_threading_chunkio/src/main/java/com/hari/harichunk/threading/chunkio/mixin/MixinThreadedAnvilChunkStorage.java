package com.hari.harichunk.threading.chunkio.mixin;

import com.ibm.asyncutil.locks.AsyncNamedLock;
import com.hari.harichunk.base.common.GlobalExecutors;
import com.hari.harichunk.base.common.registry.SerializerAccess;
import com.hari.harichunk.base.common.theinterface.IDirectStorage;
import com.hari.harichunk.base.common.util.SneakyThrow;
import com.hari.harichunk.base.mixin.access.IVersionedChunkStorage;
import com.hari.harichunk.threading.chunkio.common.AsyncSerializationManager;
import com.hari.harichunk.threading.chunkio.common.BlendingInfoUtil;
import com.hari.harichunk.threading.chunkio.common.ChunkIoMainThreadTaskUtils;
import com.hari.harichunk.threading.chunkio.common.Config;
import com.hari.harichunk.threading.chunkio.common.IAsyncChunkStorage;
import com.hari.harichunk.threading.chunkio.common.ISerializingRegionBasedStorage;
import com.hari.harichunk.threading.chunkio.common.ProtoChunkExtension;
import com.hari.harichunk.threading.chunkio.common.TaskCancellationException;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage extends ChunkStorage implements ChunkHolder.PlayerProvider {

    public MixinThreadedAnvilChunkStorage(Path path, DataFixer dataFixer, boolean bl) {
        super(path, dataFixer, bl);
    }

    @Shadow(remap = false)
    @Final
    private ServerLevel f_140133_; // level

    @Shadow(remap = false)
    @Final
    private PoiManager f_140138_; // poiManager

    @Shadow(remap = false)
    protected abstract byte m_140229_(ChunkPos chunkPos, ChunkStatus.ChunkType chunkType); // markPosition

    @Shadow(remap = false)
    @Final
    private static Logger f_140128_; // LOGGER

    @Shadow(remap = false)
    protected abstract void m_140422_(ChunkPos chunkPos); // markPositionReplaceable

    @Shadow(remap = false)
    @Final
    private Supplier<DimensionDataStorage> f_140137_; // overworldDataStorage

    @Shadow(remap = false)
    @Final
    private BlockableEventLoop<Runnable> f_140135_; // mainThreadExecutor

    @Shadow(remap = false)
    protected abstract boolean m_140425_(ChunkPos chunkPos); // isExistingChunkFull

    @Shadow(remap = false)
    private ChunkGenerator f_140136_; // generator

    @Shadow(remap = false)
    protected abstract boolean m_140258_(ChunkAccess chunk); // save

    @Shadow(remap = false)
    protected abstract void m_140318_(boolean flush); // saveAllChunks

    @Shadow(remap = false)
    private static boolean m_214940_(CompoundTag nbtCompound) { // isChunkDataValid
        throw new AbstractMethodError();
    }

    @Shadow(remap = false) protected abstract ChunkAccess m_214961_(ChunkPos chunkPos); // createEmptyChunk

    @Mutable
    @Shadow(remap = false) @Final private Long2ByteMap f_140151_; // chunkTypeCache

    @Shadow(remap = false) protected abstract CompoundTag m_214947_(CompoundTag nbt); // upgradeChunkTag

    private AsyncNamedLock<ChunkPos> chunkLock = AsyncNamedLock.createFair();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        chunkLock = AsyncNamedLock.createFair();
        this.f_140151_ = Long2ByteMaps.synchronize(this.f_140151_);
    }

    private Set<ChunkPos> scheduledChunks = new HashSet<>();

    /**
     * @author Hari
     * @reason async io and deserialization
     */
    @Overwrite(remap = false)
    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> m_140417_(ChunkPos pos) { // scheduleChunkLoad
        if (scheduledChunks == null) scheduledChunks = new HashSet<>();
        synchronized (scheduledChunks) {
            if (scheduledChunks.contains(pos)) throw new IllegalArgumentException("Already scheduled");
            scheduledChunks.add(pos);
        }

        final CompletableFuture<Optional<CompoundTag>> poiData =
                ((IAsyncChunkStorage) ((com.hari.harichunk.base.mixin.access.ISerializingRegionBasedStorage) this.f_140138_).getWorker()).getNbtAtAsync(pos)
                        .exceptionally(throwable -> {
                            //noinspection IfStatementWithIdenticalBranches
                            if (Config.recoverFromErrors) {
                                f_140128_.error("Couldn't load poi data for chunk {}, poi data will be lost!", pos, throwable);
                                return Optional.empty();
                            } else {
                                SneakyThrow.sneaky(throwable);
                                return Optional.empty(); // unreachable
                            }
                        });

        final ReferenceArrayList<Runnable> mainThreadQueue = new ReferenceArrayList<>();

        final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = getUpdatedChunkNbtAtAsync(pos)
                .thenApply(optional -> optional.filter(nbtCompound -> {
                    boolean bl = m_214940_(nbtCompound);
                    if (!bl) {
                        f_140128_.error("Chunk file at {} is missing level data, skipping", pos);
                    }

                    return bl;
                }))
                .thenApplyAsync(optional -> {
                    if (optional.isPresent()) {
                        ChunkIoMainThreadTaskUtils.push(mainThreadQueue);
                        try {
                            return ChunkSerializer.read(this.f_140133_, this.f_140138_, pos, optional.get());
                        } finally {
                            ChunkIoMainThreadTaskUtils.pop(mainThreadQueue);
                        }
                    }

                    return null;
                }, AdmanyDagScheduler.executor("chunkio-deserialize", 0.65, true))
                .exceptionally(throwable -> {
                    //noinspection IfStatementWithIdenticalBranches
                    if (Config.recoverFromErrors) {
                        f_140128_.error("Couldn't load chunk {}, chunk data will be lost!", pos, throwable);
                        return null;
                    } else {
                        SneakyThrow.sneaky(throwable);
                        return null; // unreachable
                    }
                })
//                .thenCombine(poiData, (protoChunk, tag) -> protoChunk)
//                .thenCombine(blendingInfos, (protoChunk, bitSet) -> {
//                    if (protoChunk != null) ((ProtoChunkExtension) protoChunk).setBlendingInfo(pos, bitSet);
//                    return protoChunk;
//                })
                .thenApplyAsync(protoChunk -> {
                    // blending
                    protoChunk = protoChunk != null ? protoChunk : (ProtoChunk) this.m_214961_(pos);
                    if (protoChunk.getBelowZeroRetrogen() != null || protoChunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
                        final CompletionStage<List<BitSet>> blendingInfos = BlendingInfoUtil.getBlendingInfos((IOWorker) this.chunkScanner(), pos);
                        ProtoChunk finalProtoChunk = protoChunk;
                        ((ProtoChunkExtension) protoChunk).setBlendingComputeFuture(
                                blendingInfos.thenAccept(bitSet -> ((ProtoChunkExtension) finalProtoChunk).setBlendingInfo(pos, bitSet)).toCompletableFuture()
                        );
                    }

                    ((ProtoChunkExtension) protoChunk).setInitialMainThreadComputeFuture(poiData.thenAcceptAsync(poiDataNbt -> {
                        try {
                            ((ISerializingRegionBasedStorage) this.f_140138_).update(pos, poiDataNbt.orElse(null));
                        } catch (Throwable t) {
                            if (Config.recoverFromErrors) {
                                f_140128_.error("Couldn't load poi data for chunk {}, poi data will be lost!", pos, t);
                            } else {
                                SneakyThrow.sneaky(t);
                            }
                        }
                        ChunkIoMainThreadTaskUtils.drainQueue(mainThreadQueue);
                    }, this.f_140135_));

                    this.m_140229_(pos, protoChunk.getStatus().getChunkType());
                    return Either.left(protoChunk);
                }, GlobalExecutors.invokingExecutor);
        future.exceptionally(throwable -> {
            f_140128_.error("Couldn't load chunk {}", pos, throwable);
            return null;
        });
        future.exceptionally(throwable -> null).thenRun(() -> {
            synchronized (scheduledChunks) {
                scheduledChunks.remove(pos);
            }
        });
        return future;

        // [VanillaCopy] - for reference
        /*
        return CompletableFuture.supplyAsync(() -> {
         try {
            this.world.getProfiler().visit("chunkLoad");
            CompoundTag compoundTag = this.getUpdatedChunkNbt(pos);
            if (compoundTag != null) {
               boolean bl = compoundTag.contains("Level", 10) && compoundTag.getCompound("Level").contains("Status", 8);
               if (bl) {
                  Chunk chunk = ChunkSerializer.deserialize(this.world, this.structureManager, this.pointOfInterestStorage, pos, compoundTag);
                  this.method_27053(pos, chunk.getStatus().getChunkType());
                  return Either.left(chunk);
               }

               LOGGER.error((String)"Chunk file at {} is missing level data, skipping", (Object)pos);
            }
         } catch (CrashException var5) {
            Throwable throwable = var5.getCause();
            if (!(throwable instanceof IOException)) {
               this.method_27054(pos);
               throw var5;
            }

            LOGGER.error((String)"Couldn't load chunk {}", (Object)pos, (Object)throwable);
         } catch (Exception var6) {
            LOGGER.error((String)"Couldn't load chunk {}", (Object)pos, (Object)var6);
         }

         this.method_27054(pos);
         return Either.left(new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA, this.world));
      }, this.mainThreadExecutor);
         */
    }

    private CompletableFuture<Optional<CompoundTag>> getUpdatedChunkNbtAtAsync(ChunkPos pos) {
        return m_214963_(pos);
    }

    /**
     * @author Hari
     * @reason skip datafixer if possible
     */
    @Overwrite(remap = false)
    public CompletableFuture<Optional<CompoundTag>> m_214963_(ChunkPos chunkPos) { // readChunk
//        return this.getNbt(chunkPos).thenApplyAsync(nbt -> nbt.map(this::updateChunkNbt), Util.getMainWorkerExecutor());
        return this.read(chunkPos).thenCompose(nbt -> {
            if (nbt.isPresent()) {
                final CompoundTag compound = nbt.get();
                if (ChunkStorage.getVersion(compound) != SharedConstants.getCurrentVersion().getDataVersion().getVersion()) {
                    return CompletableFuture.supplyAsync(() -> Optional.of(m_214947_(compound)), Util.backgroundExecutor());
                } else {
                    return CompletableFuture.completedFuture(nbt);
                }
            } else {
                return CompletableFuture.completedFuture(Optional.empty());
            }
        });
    }

    @ModifyReturnValue(method = "schedule", at = @At("RETURN"))
    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> postGetChunk(CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> originalReturn, ChunkHolder holder, ChunkStatus requiredStatus) {
        if (requiredStatus == ChunkStatus.FULL.getParent()) {
            // wait for initial main thread tasks before proceeding to finish full chunk
            return originalReturn.thenCompose(either -> {
                if (either.left().isPresent()) {
                    final ChunkAccess chunk = either.left().get();
                    if (chunk instanceof ProtoChunk protoChunk) {
                        final CompletableFuture<Void> future = ((ProtoChunkExtension) protoChunk).getInitialMainThreadComputeFuture();
                        if (future != null) {
                            return future.thenApply(v -> either);
                        }
                    }
                }
                return CompletableFuture.completedFuture(either);
            });
        }
        return originalReturn;
    }

    private ConcurrentLinkedQueue<CompletableFuture<Void>> saveFutures = new ConcurrentLinkedQueue<>();

    @Dynamic
    @Redirect(method = "m_202998_", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z"))
    // method: consumer in tryUnloadChunk
    private boolean asyncSave(ChunkMap tacs, ChunkAccess chunk, ChunkHolder holder) {
        // TODO [VanillaCopy] - check when updating minecraft version
        this.f_140138_.flush(chunk.getPos());
        if (!chunk.isUnsaved()) {
            return false;
        } else {
            chunk.setUnsaved(false);
            ChunkPos chunkPos = chunk.getPos();

            try {
                ChunkStatus chunkStatus = chunk.getStatus();
                if (chunkStatus.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    if (this.m_140425_(chunkPos)) {
                        return false;
                    }

                    if (chunkStatus == ChunkStatus.EMPTY && chunk.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return false;
                    }
                }

                final CompletableFuture<ChunkAccess> originalSavingFuture = holder.getChunkToSave();
                if (!originalSavingFuture.isDone()) {
                    originalSavingFuture.handleAsync((_unused, __unused) -> asyncSave(tacs, chunk, holder), this.f_140135_);
                    return false;
                }

                this.f_140133_.getProfiler().incrementCounter("chunkSave");
                // HariChunk start - async serialization
                if (saveFutures == null) saveFutures = new ConcurrentLinkedQueue<>();
                AsyncSerializationManager.Scope scope = new AsyncSerializationManager.Scope(chunk, f_140133_);

                saveFutures.add(chunkLock.acquireLock(chunk.getPos()).toCompletableFuture().thenCompose(lockToken ->
                        CompletableFuture.supplyAsync(() -> {
                                    scope.open();
                                    if (holder.getChunkToSave() != originalSavingFuture) {
                                        this.f_140135_.execute(() -> asyncSave(tacs, chunk, holder));
                                        throw new TaskCancellationException();
                                    }
                                    AsyncSerializationManager.push(scope);
                                    try {
                                        return SerializerAccess.getSerializer().serialize(f_140133_, chunk);
                                    } finally {
                                        AsyncSerializationManager.pop(scope);
                                    }
                                }, AdmanyDagScheduler.executor("chunkio-save-serialize", 0.45, false))
                                .thenAccept((either) -> {
                                    if (either.left().isPresent()) {
                                        this.write(chunkPos, either.left().get());
                                    } else {
                                        ((IDirectStorage) ((IVersionedChunkStorage) this).getWorker()).setRawChunkData(chunkPos, either.right().get());
                                    }
                                })
                                .handle((unused, throwable) -> {
                                    lockToken.releaseLock();
                                    if (throwable != null) {
                                        Throwable actual = throwable;
                                        while (actual instanceof CompletionException e) actual = e.getCause();
                                        if (!(actual instanceof TaskCancellationException)) {
                                            f_140128_.error("Failed to save chunk {},{} asynchronously, falling back to sync saving", chunkPos.x, chunkPos.z, throwable);
                                            final CompletableFuture<ChunkAccess> savingFuture = holder.getChunkToSave();
                                            if (savingFuture != originalSavingFuture) {
                                                savingFuture.handleAsync((_unused, __unused) -> m_140258_(chunk), this.f_140135_);
                                            } else {
                                                this.f_140135_.execute(() -> this.m_140258_(chunk));
                                            }
                                        }
                                    }
                                    return unused;
                                })
                ));
                this.m_140229_(chunkPos, chunkStatus.getChunkType());
                // HariChunk end
                return true;
            } catch (Exception var5) {
                f_140128_.error((String) "Failed to save chunk {},{}", (Object) chunkPos.x, chunkPos.z, var5);
                return false;
            }
        }
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        GlobalExecutors.executor.execute(() -> saveFutures.removeIf(CompletableFuture::isDone));
    }

    @Override
    public void flushWorker() {
        final CompletableFuture<Void> future = CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]));
        this.f_140135_.managedBlock(future::isDone); // wait for serialization to complete
        super.flushWorker();
    }
}
