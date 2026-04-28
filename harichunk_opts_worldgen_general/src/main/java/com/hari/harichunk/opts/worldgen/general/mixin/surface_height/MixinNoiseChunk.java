package com.hari.harichunk.opts.worldgen.general.mixin.surface_height;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.admany.vkgpuaccel.VkSurfaceHeightCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pre-fills {@code NoiseChunk.preliminarySurfaceLevelCache} with GPU-estimated
 * surface heights sourced from {@link VkSurfaceHeightCache}.
 *
 * <p>The injection runs at the end of the {@code NoiseChunk} constructor, after
 * {@code preliminarySurfaceLevelCache} is initialised but before any of the
 * lazy {@code computeIfAbsent} calls that would otherwise trigger CPU density-
 * function evaluations during {@code fillFromNoise}.  When an entry is already
 * present (set here), the {@code computeIfAbsent} short-circuits and the CPU
 * computation is skipped entirely for that quart position.
 *
 * <p>If no injection is pending for this chunk (GPU was unavailable or the
 * async batch had not completed) the method is a no-op and generation proceeds
 * unchanged.
 */
@Mixin(NoiseChunk.class)
public class MixinNoiseChunk {

    @Shadow(remap = false) @Final private int f_188723_;  // firstNoiseX
    @Shadow(remap = false) @Final private int f_188724_;  // firstNoiseZ
    @Shadow(remap = false) @Final private Long2IntMap f_198238_;  // preliminarySurfaceLevelCache

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hc$injectGpuSurfaceHeights(CallbackInfo ci) {
        int[] heights = VkSurfaceHeightCache.takePendingInjection(this.f_188723_, this.f_188724_);
        if (heights == null) return;

        // Fill the 4×4 quart positions belonging to this chunk.
        // firstNoiseX = chunkX * 4, so the absolute quart-X for local index lqx is
        // firstNoiseX + lqx, and its block-X is (firstNoiseX + lqx) << 2.
        for (int lqx = 0; lqx < 4; lqx++) {
            for (int lqz = 0; lqz < 4; lqz++) {
                int blockX = QuartPos.toBlock(this.f_188723_ + lqx);
                int blockZ = QuartPos.toBlock(this.f_188724_ + lqz);
                long key = ColumnPos.asLong(blockX, blockZ);
                int height = heights[lqx * 4 + lqz];
                // putIfAbsent: preserve any value the constructor may have already computed.
                f_198238_.putIfAbsent(key, height);
            }
        }
    }
}
