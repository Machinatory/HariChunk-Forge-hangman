package com.hari.harichunk.opts.scheduling;

import com.hari.harichunk.opts.scheduling.common.Config;
import org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler;
import net.minecraftforge.fml.common.Mod;

@Mod("harichunk_opts_scheduling")
public class ModuleEntryPoint {

    private static final boolean enabled = true;

    static {
        Config.init();
    }

    public ModuleEntryPoint() {
        try {
            AdmanyDagScheduler.initialize();
        } catch (Throwable t) {
            // Non-critical: falls back to standard scheduling
        }
    }

}
