package com.hari.harichunk.opts.chunkio;

import com.hari.harichunk.opts.chunkio.common.Config;
import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_opts_chunkio")
public class ModuleEntryPoint {

    private static final boolean enabled = true;

    static {
        Config.init();
    }

}
