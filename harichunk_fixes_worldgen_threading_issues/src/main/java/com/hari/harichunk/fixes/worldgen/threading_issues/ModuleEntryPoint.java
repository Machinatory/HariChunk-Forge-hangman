package com.hari.harichunk.fixes.worldgen.threading_issues;

import com.hari.harichunk.fixes.worldgen.threading_issues.common.Config;
import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_fixes_worldgen_threading_issues")
public class ModuleEntryPoint {

    private static final boolean enabled = true;

    static {
        Config.init();
    }

}
