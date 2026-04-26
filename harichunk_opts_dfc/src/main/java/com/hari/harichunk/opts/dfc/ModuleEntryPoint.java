package com.hari.harichunk.opts.dfc;

import com.hari.harichunk.base.common.config.ConfigSystem;
import com.hari.harichunk.opts.dfc.common.gen.GpuDensityFunction;
import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_opts_dfc")
public class ModuleEntryPoint {

    public static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.useDensityFunctionCompiler")
            .comment("Whether to use density function compiler to accelerate world generation. "
                    + "Density function: https://minecraft.wiki/w/Density_function. "
                    + "This functionality compiles density functions from world generation "
                    + "datapacks (including vanilla generation) to JVM bytecode to increase "
                    + "performance by allowing JVM JIT to better optimize the code. "
                    + "Currently, all functions provided by vanilla are implemented. "
                    + "Chunk upgrades from pre-1.18 versions are not implemented and will "
                    + "fall back to the unoptimized version of density functions.")
            .getBoolean(true, false);

    public static final boolean gpuAccelerationEnabled = Boolean.parseBoolean(
            System.getProperty("harichunk.dfc.gpu_acceleration", "true")
    );

    public ModuleEntryPoint() {
        init();
    }

    public static void init() {
        if (!enabled) return;

        if (gpuAccelerationEnabled) {
            try {
                GpuDensityFunction.initialize();
            } catch (Throwable t) {
                // Non-critical: GPU acceleration is optional
            }
        }
    }

}
