package com.hari.harichunk.threading.lighting;

import net.minecraftforge.fml.common.Mod;
import net.sjhub.harichunk.utils.ModUtil;

@Mod("harichunk_threading_lighting")
public class ModuleEntryPoint {

    private static final boolean enabled = !ModUtil.isModLoaded("lightbench");
}
