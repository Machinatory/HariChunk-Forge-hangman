package com.hari.harichunk.threading.chunkio;

import com.hari.harichunk.base.common.config.ConfigSystem;
import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_threading_chunkio")
public class ModuleEntryPoint {

    private static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("ioSystem.async")
            .comment("Whether to use async chunk loading & unloading")
            .incompatibleMod("radon", "*")
            .getBoolean(true, false);

}
