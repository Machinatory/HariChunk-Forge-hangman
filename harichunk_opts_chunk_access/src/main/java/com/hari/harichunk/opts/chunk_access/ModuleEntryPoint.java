package com.hari.harichunk.opts.chunk_access;

import com.hari.harichunk.base.common.config.ConfigSystem;
import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_opts_chunk_access")
public class ModuleEntryPoint {

    public static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("generalOptimizations.optimizeAsyncChunkRequest")
            .comment("Whether to let async chunk request no longer block server thread\n" +
                    "(may cause incompatibility with other mods)")
            .getBoolean(true, false);

}
