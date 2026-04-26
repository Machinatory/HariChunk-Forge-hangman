package com.hari.harichunk.threading.worldgen.common;

import com.google.common.base.Preconditions;
import com.ibm.asyncutil.locks.AsyncLock;
import com.ibm.asyncutil.locks.AsyncNamedLock;
import com.hari.harichunk.base.common.scheduler.NeighborLockingTask;
import com.hari.harichunk.base.common.scheduler.SchedulingManager;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler;

import static com.hari.harichunk.threading.worldgen.common.ChunkStatusUtils.ChunkStatusThreadingType.AS_IS;
import static com.hari.harichunk.threading.worldgen.common.ChunkStatusUtils.ChunkStatusThreadingType.PARALLELIZED;
import static com.hari.harichunk.threading.worldgen.common.ChunkStatusUtils.ChunkStatusThreadingType.SINGLE_THREADED;

public class ChunkStatusUtils {

    public static final BooleanSupplier FALSE_SUPPLIER = () -> false;

    public static ChunkStatusThreadingType getThreadingType(final ChunkStatus status) {
        if (status.equals(ChunkStatus.STRUCTURE_STARTS)
                || status.equals(ChunkStatus.STRUCTURE_REFERENCES)
                || status.equals(ChunkStatus.BIOMES)
                || status.equals(ChunkStatus.NOISE)
                || status.equals(ChunkStatus.SPAWN)
                || status.equals(ChunkStatus.SURFACE)
                || status.equals(ChunkStatus.CARVERS)) {
            return PARALLELIZED;
        } else if (status.equals(ChunkStatus.FEATURES)) {
            return Config.allowThreadedFeatures ? PARALLELIZED : SINGLE_THREADED;
        } else if (status.equals(ChunkStatus.INITIALIZE_LIGHT) ||
                   status.equals(ChunkStatus.LIGHT)) {
            return AS_IS;
        }
        return AS_IS;
    }

    public static <T> CompletableFuture<T> runChunkGenWithLock(ChunkPos target, ChunkStatus status, ChunkHolder holder, int radius, SchedulingManager schedulingManager, boolean async, AsyncNamedLock<ChunkPos> chunkLock, Supplier<CompletableFuture<T>> action) {
        Preconditions.checkNotNull(status);
//        if (radius == 0)
//            return StageSupport.tryWith(chunkLock.acquireLock(target), unused -> action.get()).toCompletableFuture().thenCompose(Function.identity());

        BooleanSupplier isCancelled;

        if (holder != null) {
            isCancelled = () -> isCancelled(holder, status);
        } else {
            isCancelled = FALSE_SUPPLIER;
        }

//        ArrayList<ChunkPos> fetchedLocks = new ArrayList<>((2 * radius + 1) * (2 * radius + 1));
//        for (int x = target.x - radius; x <= target.x + radius; x++)
//            for (int z = target.z - radius; z <= target.z + radius; z++)
//                fetchedLocks.add(new ChunkPos(x, z));
//
//        final SchedulingAsyncCombinedLock<T> task = new SchedulingAsyncCombinedLock<>(
//                chunkLock,
//                target.toLong(),
//                new HashSet<>(fetchedLocks),
//                isCancelled,
//                schedulingManager::enqueue,
//                action,
//                target.toString(),
//                async);

        LongArrayList lockTargets = new LongArrayList((2 * radius + 1) * (2 * radius + 1));
        for (int x = target.x - radius; x <= target.x + radius; x++)
            for (int z = target.z - radius; z <= target.z + radius; z++)
                lockTargets.add(ChunkPos.asLong(x, z));

        final NeighborLockingTask<T> task = new NeighborLockingTask<>(
                schedulingManager,
                target.toLong(),
                lockTargets.toLongArray(),
                isCancelled,
                action,
                "%s %s".formatted(target.toString(), status.toString()),
                async
        );
        return task.getFuture();
    }

    public static boolean isCancelled(ChunkHolder holder, ChunkStatus targetStatus) {
        return ChunkLevel.generationStatus(holder.getTicketLevel()).getIndex() < targetStatus.getIndex();
    }

    public enum ChunkStatusThreadingType {

        PARALLELIZED() {
            @Override
            public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture) {
                return AdmanyDagScheduler
                        .submitChunkTask("worldgen-parallelized", 0.55, false, completableFuture)
                        .thenCompose(Function.identity());
            }
        },
        SINGLE_THREADED() {
            @Override
            public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture) {
                Preconditions.checkNotNull(lock);
                return lock.acquireLock().toCompletableFuture().thenCompose(lockToken ->
                        AdmanyDagScheduler
                                .submitChunkTask("worldgen-locked", 0.75, true, () -> {
                                    try {
                                        return completableFuture.get();
                                    } finally {
                                        lockToken.releaseLock();
                                    }
                                })
                                .thenCompose(Function.identity()));
            }
        },
        AS_IS() {
            @Override
            public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture) {
                return completableFuture.get();
            }
        };

        public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture);

    }
}
