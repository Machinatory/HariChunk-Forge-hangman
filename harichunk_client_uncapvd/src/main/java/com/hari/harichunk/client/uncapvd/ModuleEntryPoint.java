package com.hari.harichunk.client.uncapvd;

import com.hari.harichunk.base.common.config.ConfigSystem;
import com.hari.harichunk.client.uncapvd.common.Config;
import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_client_uncapvd")
public class ModuleEntryPoint {

    private static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("clientSideConfig.modifyMaxVDConfig.enabled")
            .comment("Whether to modify maximum view distance")
            .incompatibleMod("bobby", "*")
            .getBoolean(true, false);

    static {
        Config.init();
    }

}
