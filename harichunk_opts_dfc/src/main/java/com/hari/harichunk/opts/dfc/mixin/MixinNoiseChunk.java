package com.hari.harichunk.opts.dfc.mixin;

import com.hari.harichunk.opts.dfc.common.ducks.IArrayCacheCapable;
import com.hari.harichunk.opts.dfc.common.ducks.ICoordinatesFilling;
import com.hari.harichunk.opts.dfc.common.gen.DelegatingBlendingAwareVisitor;
import com.hari.harichunk.opts.dfc.common.util.ArrayCache;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk implements IArrayCacheCapable, ICoordinatesFilling {

    @Shadow(remap = false) @Final private int f_188719_; // cellCountY
    @Shadow(remap = false) @Final private int f_188718_; // cellCountXZ
    @Shadow(remap = false) private int f_209150_; // cellStartBlockX
    @Shadow(remap = false) private int f_209151_; // cellStartBlockY
    @Shadow(remap = false) private int f_209152_; // cellStartBlockZ

    @Shadow(remap = false) private boolean f_209172_; // interpolating

    private final ArrayCache harichunk$arrayCache = new ArrayCache();

    @Override
    public ArrayCache harichunk$getArrayCache() {
        return this.harichunk$arrayCache != null ? this.harichunk$arrayCache : new ArrayCache();
    }

    @Override
    public void harichunk$fillCoordinates(int[] x, int[] y, int[] z) {
        int index = 0;
        for (int i = this.f_188719_; i >= 0; i--) {
            int blockY = this.f_209151_ + i;
            for (int j = 0; j < this.f_188718_; j++) {
                int blockX = this.f_209150_ + j;
                for (int k = 0; k < this.f_188718_; k++) {
                    int blockZ = this.f_209152_ + k;

                    x[index] = blockX;
                    y[index] = blockY;
                    z[index] = blockZ;

                    index++;
                }
            }
        }
    }

    @Unique
    private @NotNull DelegatingBlendingAwareVisitor harichunk$getDelegatingBlendingAwareVisitor(DensityFunction.Visitor visitor, boolean blendingEnabled) {
        return new DelegatingBlendingAwareVisitor(visitor, blendingEnabled);
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"))
    private DensityFunction.Visitor modifyRouterVisitor(DensityFunction.Visitor visitor) {
        // Check if blending is enabled - in 1.20.1, NoiseChunk has a blender field
        // When blender is the no-blending implementation, blending is disabled
        boolean blendingEnabled = harichunk$isBlendingEnabled();
        return harichunk$getDelegatingBlendingAwareVisitor(visitor, blendingEnabled);
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/DensityFunction;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/DensityFunction;"), require = 1, expect = 1)
    private DensityFunction.Visitor modifyDensityFunctionVisitor(DensityFunction.Visitor visitor) {
        boolean blendingEnabled = harichunk$isBlendingEnabled();
        return harichunk$getDelegatingBlendingAwareVisitor(visitor, blendingEnabled);
    }

    @Unique
    private boolean harichunk$isBlendingEnabled() {
        // In 1.20.1, NoiseChunk stores a Blender field.
        // The no-blending case is when the blender is an empty/no-op implementation.
        // We determine this by checking if the NoiseChunk is within blending range.
        // For simplicity, we always enable the blending-aware visitor since
        // the DelegatingBlendingAwareVisitor handles both cases correctly.
        return true;
    }

}
