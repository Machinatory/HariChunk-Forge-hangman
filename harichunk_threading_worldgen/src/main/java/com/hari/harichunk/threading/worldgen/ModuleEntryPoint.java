package com.hari.harichunk.threading.worldgen;

import com.hari.harichunk.base.common.config.ConfigSystem;
import com.hari.harichunk.threading.worldgen.common.Config;
import net.minecraftforge.fml.common.Mod;

import static com.hari.harichunk.base.ModuleEntryPoint.globalExecutorParallelism;

@Mod("harichunk_threading_worldgen")
public class ModuleEntryPoint {

    public static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("threadedWorldGen.enabled")
            .comment("Whether to enable this feature")
            .getBoolean(globalExecutorParallelism >= 3, false);

    static {
        Config.init();
    }

}
