package com.hari.harichunk.opts.allocs.mixin.noise;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;

@Mixin(MaterialRuleList.class)
public class MixinChainedBlockSource {

    @Unique
    private NoiseChunk.BlockStateFiller[] samplersArray;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(List<NoiseChunk.BlockStateFiller> samplers, CallbackInfo ci) {
        this.samplersArray = samplers.toArray(NoiseChunk.BlockStateFiller[]::new);
    }

    /**
     * @author Hari
     * @reason reduce allocs using array
     */
    @Overwrite(remap = false)
    public @Nullable BlockState m_207387_(DensityFunction.FunctionContext arg) {
        // TODO [VanillaCopy]
        for (NoiseChunk.BlockStateFiller blockStateSampler : this.samplersArray) {
            BlockState blockState = blockStateSampler.calculate(arg);
            if (blockState != null) {
                return blockState;
            }
        }

        return null;
    }

}
