package com.hari.harichunk.opts.natives_math;

import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_natives_opts")
public class ModuleEntryPoint {

    public static final boolean enabled;
    public static final boolean allowAVX512;

    static {
        enabled = Boolean.parseBoolean(System.getProperty("harichunk.natives_math.enabled", "true"));
        allowAVX512 = Boolean.parseBoolean(System.getProperty("harichunk.natives_math.allowAVX512", "false"));
    }

}
