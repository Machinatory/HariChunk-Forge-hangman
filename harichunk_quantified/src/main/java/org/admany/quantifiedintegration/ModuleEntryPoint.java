package org.admany.quantifiedintegration;

import net.minecraftforge.fml.common.Mod;
import org.admany.quantifiedintegration.config.Config;
import org.admany.quantifiedintegration.opencl.OpenCLSafetyInterceptor;
import org.admany.quantifiedintegration.opencl.QAPIThreadClassloaderFix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("harichunk_x_quantified_api")
public final class ModuleEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("Quantified API Integration");

    public static boolean enabled = Config.ENABLE_QUANTIFIED;

    public ModuleEntryPoint() {
        if (!enabled) {
            LOGGER.info("Quantified API integration disabled by config");
            return;
        }

        if (QuantifiedIntegration.register()) {
            LOGGER.info("Quantified API integration ready");
            OpenCLSafetyInterceptor.intercept(LOGGER);
            QAPIThreadClassloaderFix.fix(LOGGER);
        } else {
            LOGGER.warn("Quantified API integration could not register with Quantified API");
        }
    }
}
