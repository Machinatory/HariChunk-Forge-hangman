package com.hari.harichunk.opts.allocs.mixin.surfacebuilder;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

@Mixin(SurfaceRules.Context.class)
public class MixinMaterialRuleContext {

    @Shadow(remap = false)
    @Final
    private Function<BlockPos, Holder<Biome>> f_189542_;  // biomeGetter

    @Shadow(remap = false)
    @Final
    private BlockPos.MutableBlockPos f_189554_;  // pos

    @Shadow(remap = false)
    private long f_189553_;  // lastUpdateY

    @Shadow(remap = false)
    private Supplier<Holder<Biome>> f_189555_;  // biome

    @Shadow(remap = false)
    private int f_189557_;  // blockY

    @Shadow(remap = false)
    private int f_189558_;  // waterHeight

    @Shadow(remap = false)
    private int f_189559_;  // stoneDepthBelow

    @Shadow(remap = false)
    private int f_189560_;  // stoneDepthAbove

    @Unique
    private int lazyPosX;
    @Unique
    private int lazyPosY;
    @Unique
    private int lazyPosZ;
    @Unique
    private Holder<Biome> lastBiome = null;
    @Unique
    private ResourceKey<Biome> lastBiomeKey = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.f_189555_ = () -> {
            if (this.lastBiome == null)
                return this.lastBiome = this.f_189542_.apply(this.f_189554_.set(this.lazyPosX, this.lazyPosY, this.lazyPosZ));
            return this.lastBiome;
        };
    }

    /**
     * @author Hari
     * @reason reduce allocs
     */
    @Overwrite(remap = false)
    public void m_189576_(int i, int j, int k, int l, int m, int n) {
        // TODO [VanillaCopy]
        ++this.f_189553_;
        this.f_189557_ = m;
        this.f_189558_ = k;
        this.f_189559_ = j;
        this.f_189560_ = i;

        // set lazy values
        this.lazyPosX = l;
        this.lazyPosY = m;
        this.lazyPosZ = n;
        // clear cache
        this.lastBiome = null;
        this.lastBiomeKey = null;
    }

}
