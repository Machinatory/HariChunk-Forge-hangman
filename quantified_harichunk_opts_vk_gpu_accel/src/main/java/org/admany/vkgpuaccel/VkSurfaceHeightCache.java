/*
 * HariChunk Quantified Vulkan GPU Acceleration by Admany / BlackRift Studios.
 * Licensed strictly under BRSSLA V1.5.
 * This is original BlackRift Studios code and is not related to C2ME in any way.
 *
 * This source may be viewed and contributed to through approved project channels.
 * Modification, copying, redistribution, reuse, or derivative use outside those
 * approved contribution flows is strictly not allowed. C2ME may not copy, modify,
 * reuse, redistribute, or derive from this code.
 */
package org.admany.vkgpuaccel;

import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GPU-backed surface height pre-cache for Minecraft chunk noise generation.
 *
 * <p>Before the NOISE chunk status runs, {@link #warmAsync(ChunkPos)} is called
 * during STRUCTURE_REFERENCES to fire off a Vulkan terrain-feature batch for
 * the 4×4 chunk region containing the target chunk.  The 16×16 estimated
 * surface-height values are stored asynchronously.
 *
 * <p>Immediately before the NOISE generation supplier is invoked,
 * {@link #injectIfReady(ChunkPos)} checks whether the async result is already
 * complete.  If it is, the per-chunk 4×4 quart-position slice is placed into
 * {@link #PENDING_INJECTION} keyed by the chunk's packed {@link ChunkPos}.
 *
 * <p>The companion Mixin {@code MixinNoiseChunk} reads from {@link #PENDING_INJECTION}
 * at the end of {@code NoiseChunk.<init>} and pre-fills
 * {@code NoiseChunk.preliminarySurfaceLevelCache} so that
 * {@code computeIfAbsent} calls during {@code fillFromNoise} find existing
 * entries and skip the CPU density-function computation entirely.
 */
public final class VkSurfaceHeightCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk/VkSurfaceHeightCache");

    /** Number of chunks per region side (matches the DAG locality key grouping). */
    public static final int REGION_CHUNKS = 4;
    /** Quart positions per chunk side (1 quart = 4 blocks, 1 chunk = 16 blocks → 4 quarts). */
    public static final int QUARTS_PER_CHUNK = 4;
    /** Total quart positions per region side. */
    public static final int REGION_QUARTS = REGION_CHUNKS * QUARTS_PER_CHUNK; // 16
    /** Total height samples per region. */
    public static final int REGION_TOTAL = REGION_QUARTS * REGION_QUARTS;    // 256

    /**
     * Async region cache: packed (regionX, regionZ) → future of 256 surface-height ints.
     * A null result means the GPU run failed / was skipped; the entry stays in the map
     * to avoid redundant retries within the same generation session.
     */
    private static final ConcurrentHashMap<Long, CompletableFuture<int[]>> REGION_CACHE =
            new ConcurrentHashMap<>(512);

    /**
     * Per-chunk injection staging map: packed (chunkX, chunkZ) → 16 height ints.
     * Populated by {@link #injectIfReady(ChunkPos)}, consumed (and removed) by
     * {@link #takePendingInjection(int, int)}.
     */
    public static final ConcurrentHashMap<Long, int[]> PENDING_INJECTION =
            new ConcurrentHashMap<>(256);

    private VkSurfaceHeightCache() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Fires an async GPU batch for the 4×4-chunk region that contains {@code pos}.
     * Safe to call multiple times for the same region; only the first call starts
     * the GPU work.  Silently no-ops if Vulkan is not ready.
     */
    public static void warmAsync(ChunkPos pos) {
        if (!VkGpuAccel.isReady()) return;
        int regionX = pos.x >> 2;
        int regionZ = pos.z >> 2;
        long regionKey = ChunkPos.asLong(regionX, regionZ);
        REGION_CACHE.computeIfAbsent(regionKey, k -> computeRegionAsync(regionX, regionZ));
    }

    /**
     * If the region future for {@code pos} has already completed, extracts the
     * 4×4 quart-position slice for that specific chunk and places it in
     * {@link #PENDING_INJECTION} so the NoiseChunk mixin can consume it.
     * Non-blocking; silently skips if the future is pending or returned null.
     */
    public static void injectIfReady(ChunkPos pos) {
        int regionX = pos.x >> 2;
        int regionZ = pos.z >> 2;
        long regionKey = ChunkPos.asLong(regionX, regionZ);

        CompletableFuture<int[]> future = REGION_CACHE.get(regionKey);
        if (future == null || !future.isDone()) return;

        int[] regionHeights = future.getNow(null);
        if (regionHeights == null || regionHeights.length != REGION_TOTAL) return;

        // Compute this chunk's position within its 4×4 region (0..3 in each axis)
        int chunkRx = pos.x - regionX * REGION_CHUNKS; // 0..3
        int chunkRz = pos.z - regionZ * REGION_CHUNKS; // 0..3

        // Extract the 4×4 quart positions belonging to this chunk
        int[] chunkHeights = new int[QUARTS_PER_CHUNK * QUARTS_PER_CHUNK];
        for (int lqx = 0; lqx < QUARTS_PER_CHUNK; lqx++) {
            for (int lqz = 0; lqz < QUARTS_PER_CHUNK; lqz++) {
                int regionQx = chunkRx * QUARTS_PER_CHUNK + lqx;
                int regionQz = chunkRz * QUARTS_PER_CHUNK + lqz;
                chunkHeights[lqx * QUARTS_PER_CHUNK + lqz] =
                        regionHeights[regionQx * REGION_QUARTS + regionQz];
            }
        }
        PENDING_INJECTION.put(pos.toLong(), chunkHeights);
    }

    /**
     * Called from the NoiseChunk mixin.  Removes and returns the pending
     * injection array for the chunk whose first-noise quart positions are
     * {@code firstNoiseX} and {@code firstNoiseZ}.
     *
     * @param firstNoiseX the {@code firstNoiseX} field value of the NoiseChunk
     *                    (= {@code chunkX * 4} for standard Minecraft chunks)
     * @param firstNoiseZ the {@code firstNoiseZ} field value of the NoiseChunk
     * @return a 16-element int array [lqx * 4 + lqz] with estimated surface Y
     *         values, or {@code null} if no injection is pending for this chunk
     */
    public static int[] takePendingInjection(int firstNoiseX, int firstNoiseZ) {
        // firstNoiseX = chunkX * 4  →  chunkX = firstNoiseX >> 2
        int chunkX = firstNoiseX >> 2;
        int chunkZ = firstNoiseZ >> 2;
        return PENDING_INJECTION.remove(ChunkPos.asLong(chunkX, chunkZ));
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /**
     * Submits a Vulkan terrain-feature batch covering the 16×16 quart positions
     * of the 4×4-chunk region at ({@code regionX}, {@code regionZ}).
     * Returns a future of 256 surface-height ints, or a completed-null future on
     * GPU failure/rejection.
     */
    private static CompletableFuture<int[]> computeRegionAsync(int regionX, int regionZ) {
        // Pack the 16×16 = 256 quart positions as (blockX, 0, blockZ) float triples.
        // Row-major: outer index = qx (0..15), inner = qz (0..15).
        float[] inputCoords = new float[REGION_TOTAL * 3];
        for (int qx = 0; qx < REGION_QUARTS; qx++) {
            for (int qz = 0; qz < REGION_QUARTS; qz++) {
                int absQx = regionX * REGION_QUARTS + qx;
                int absQz = regionZ * REGION_QUARTS + qz;
                int blockX = absQx << 2; // QuartPos.toBlock = * 4
                int blockZ = absQz << 2;
                int idx = (qx * REGION_QUARTS + qz) * 3;
                inputCoords[idx]     = blockX;
                inputCoords[idx + 1] = 0.0f;  // Y = ground reference for height estimation
                inputCoords[idx + 2] = blockZ;
            }
        }

        CompletableFuture<float[]> gpuFuture = VkGpuAccel.submitTerrainFeatureBatch(inputCoords);
        return gpuFuture.thenApply(floatHeights -> {
            if (floatHeights == null || floatHeights.length < REGION_TOTAL) {
                return null;
            }
            int[] intHeights = new int[REGION_TOTAL];
            for (int i = 0; i < REGION_TOTAL; i++) {
                float h = floatHeights[i];
                // Sanity-check: valid Minecraft world heights are well within ±2048.
                // Values outside this range indicate the kernel returned non-height data;
                // abort and return null so generation falls back to CPU computation.
                if (h < -2048f || h > 2048f) {
                    return null;
                }
                intHeights[i] = (int) h;
            }
            return intHeights;
        }).exceptionally(t -> {
            LOGGER.debug("VkSurfaceHeightCache GPU batch failed for region ({}, {}): {}",
                    regionX, regionZ, t.getMessage());
            return null;
        });
    }
}
