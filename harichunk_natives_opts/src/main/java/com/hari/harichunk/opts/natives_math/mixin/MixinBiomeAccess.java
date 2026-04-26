package com.hari.harichunk.opts.natives_math.mixin;

import com.hari.harichunk.opts.natives_math.common.NativeBindings;
import com.hari.harichunk.opts.natives_math.common.NativeLoader;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BiomeManager.class)
public class MixinBiomeAccess {
    // Biome access optimization via native biomeAccessSample
    // Full integration requires deeper hooking into the biome sampling pipeline
}
