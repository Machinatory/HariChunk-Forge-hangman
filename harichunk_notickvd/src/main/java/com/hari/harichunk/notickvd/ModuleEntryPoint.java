package com.hari.harichunk.notickvd;

import com.hari.harichunk.base.common.config.ConfigSystem;
import com.hari.harichunk.notickvd.common.Config;
import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_notickvd")
public class ModuleEntryPoint {

    public static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("noTickViewDistance.enabled")
            .comment("Whether to enable no-tick view distance")
            .getBoolean(true, false);

    static {
        Config.init();
    }

}
