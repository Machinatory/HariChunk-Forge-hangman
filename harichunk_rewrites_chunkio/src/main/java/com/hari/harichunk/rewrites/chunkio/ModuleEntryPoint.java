package com.hari.harichunk.rewrites.chunkio;

import com.hari.harichunk.base.common.config.ConfigSystem;
import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_rewrites_chunkio")
public class ModuleEntryPoint {

    private static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.replaceImpl")
            .comment("Whether to use the optimized implementation of IO system")
            .getBoolean(com.hari.harichunk.base.ModuleEntryPoint.globalExecutorParallelism >= 2, false);

}
