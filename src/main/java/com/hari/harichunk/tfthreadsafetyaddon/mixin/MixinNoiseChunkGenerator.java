package com.hari.harichunk.tfthreadsafetyaddon.mixin;

import com.hari.harichunk.tfthreadsafetyaddon.common.ducks.TFBiomeProviderExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import twilightforest.world.components.biomesources.TFBiomeProvider;

@Mixin(NoiseBasedChunkGenerator.class)
public class MixinNoiseChunkGenerator {

    @org.spongepowered.asm.mixin.Unique
    private final ThreadLocal<TFBiomeProvider> tfthreadsafetyaddon$threadLocalTFBiomeProvider = new ThreadLocal<>();

    @WrapOperation(method = "doCreateBiomes", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;biomeSource:Lnet/minecraft/world/level/biome/BiomeSource;", opcode = Opcodes.GETFIELD))
    private BiomeSource recreateBiomeResolver(NoiseBasedChunkGenerator instance, Operation<BiomeSource> original) {
        BiomeSource value = original.call(instance);
        if (value instanceof TFBiomeProvider tfBiomeProvider) {
            TFBiomeProvider cached = this.tfthreadsafetyaddon$threadLocalTFBiomeProvider.get();
            if (cached == null) {
                cached = ((TFBiomeProviderExtension) tfBiomeProvider).tfthreadsafetyaddon$recreate();
                this.tfthreadsafetyaddon$threadLocalTFBiomeProvider.set(cached);
            }
            return cached;
        } else {
            return value;
        }
    }

}
