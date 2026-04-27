package com.hari.harichunk.threading.worldgen.common;

import com.google.common.base.Preconditions;
import com.ibm.asyncutil.locks.AsyncLock;
import com.ibm.asyncutil.locks.AsyncNamedLock;
import com.hari.harichunk.base.common.scheduler.NeighborLockingTask;
import com.hari.harichunk.base.common.scheduler.SchedulingManager;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.lang.reflect.Method;
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

    // Reflected handles for VkSurfaceHeightCache – optional GPU module, resolved once.
    private static volatile Method WARM_ASYNC_METHOD  = null;
    private static volatile Method INJECT_IF_READY_METHOD = null;
    private static volatile boolean surfaceCacheLookupDone = false;

    private static void ensureSurfaceCacheMethods() {
        if (surfaceCacheLookupDone) return;
        synchronized (ChunkStatusUtils.class) {
            if (surfaceCacheLookupDone) return;
            try {
                Class<?> cls = Class.forName("org.admany.vkgpuaccel.VkSurfaceHeightCache");
                WARM_ASYNC_METHOD    = cls.getMethod("warmAsync",    ChunkPos.class);
                INJECT_IF_READY_METHOD = cls.getMethod("injectIfReady", ChunkPos.class);
            } catch (Exception ignored) {
                // Module not present – surface height pre-cache disabled.
            }
            surfaceCacheLookupDone = true;
        }
    }

    private static void surfaceCacheWarm(ChunkPos pos) {
        ensureSurfaceCacheMethods();
        if (WARM_ASYNC_METHOD == null || pos == null) return;
        try { WARM_ASYNC_METHOD.invoke(null, pos); } catch (Exception ignored) {}
    }

    private static void surfaceCacheInject(ChunkPos pos) {
        ensureSurfaceCacheMethods();
        if (INJECT_IF_READY_METHOD == null || pos == null) return;
        try { INJECT_IF_READY_METHOD.invoke(null, pos); } catch (Exception ignored) {}
    }

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

    // Returns the per-stage DAG description so the QAPI can distinguish task types.
    static String dagDescriptionFor(ChunkStatus status) {
        if (status == ChunkStatus.NOISE)                return "worldgen-noise";
        if (status == ChunkStatus.BIOMES)               return "worldgen-biomes";
        if (status == ChunkStatus.SURFACE)              return "worldgen-surface";
        if (status == ChunkStatus.CARVERS)              return "worldgen-carvers";
        if (status == ChunkStatus.STRUCTURE_STARTS)     return "worldgen-structure-starts";
        if (status == ChunkStatus.STRUCTURE_REFERENCES) return "worldgen-structure-refs";
        if (status == ChunkStatus.SPAWN)                return "worldgen-spawn";
        if (status == ChunkStatus.FEATURES)             return "worldgen-features";
        return "worldgen-parallelized";
    }

    // NOISE tasks get higher priority because they feed the GPU batch queue directly.
    static double dagPriorityFor(ChunkStatus status) {
        if (status == ChunkStatus.NOISE)   return 0.70;
        if (status == ChunkStatus.BIOMES)  return 0.60;
        if (status == ChunkStatus.SURFACE) return 0.55;
        return 0.50;
    }

    // Group chunks into 4×4 regions so the QAPI batches spatially coherent work
    // (neighbouring chunks land on the GPU together, keeping mcDensityFunctionsBatch dense).
    static String localityKeyFor(ChunkStatus status, ChunkPos pos) {
        String base = dagDescriptionFor(status);
        if (pos == null) return base;
        int rx = pos.x >> 2;
        int rz = pos.z >> 2;
        return base + ":r" + rx + "." + rz;
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
            public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture, ChunkStatus status, ChunkPos pos) {
                // During STRUCTURE_REFERENCES, fire off an async GPU surface-height batch
                // for the 4x4 chunk region so the result is ready before NOISE runs.
                if (status == ChunkStatus.STRUCTURE_REFERENCES) {
                    surfaceCacheWarm(pos);
                }
                // During NOISE, move the completed GPU heights into the per-chunk injection
                // map so MixinNoiseChunk can pre-fill NoiseChunk.preliminarySurfaceLevelCache.
                if (status == ChunkStatus.NOISE) {
                    surfaceCacheInject(pos);
                }
                return AdmanyDagScheduler
                        .submitChunkTask(dagDescriptionFor(status), dagPriorityFor(status), false,
                                localityKeyFor(status, pos), completableFuture)
                        .thenCompose(Function.identity());
            }
        },
        SINGLE_THREADED() {
            @Override
            public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture, ChunkStatus status, ChunkPos pos) {
                Preconditions.checkNotNull(lock);
                String desc = dagDescriptionFor(status);
                String localityKey = localityKeyFor(status, pos);
                return lock.acquireLock().toCompletableFuture().thenCompose(lockToken ->
                        AdmanyDagScheduler
                                .submitChunkTask(desc, 0.75, true, localityKey, () -> {
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
            public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture, ChunkStatus status, ChunkPos pos) {
                return completableFuture.get();
            }
        };

        public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> runTask(AsyncLock lock, Supplier<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture, ChunkStatus status, ChunkPos pos);

    }
}
