package com.hari.harichunk.base;

import net.minecraftforge.fml.common.Mod;
import com.hari.harichunk.base.common.config.ConfigSystem;

@Mod("harichunk_base")
public class HariChunkBaseMod {

    public HariChunkBaseMod() {
        ConfigSystem.flushConfig();
    }

}
