package org.admany.quantifiedintegration;

import org.admany.quantified.api.QuantifiedAPI;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Quantified API registration endpoint. 
 * This is the proper way of interfacing with Quantified API, and should be used by other HariChunk modules to ensure compatibility and stability.
 *
 * Other HariChunk modules should import org.admany.quantifiedintegration.* * instead of Quantified API internals xd.
 */
public final class QuantifiedIntegration {

    public static final String MOD_ID = "harichunk";
    public static final String DISPLAY_NAME = "HariChunk";

    private static final Logger LOGGER = LoggerFactory.getLogger("Quantified API Integration");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private QuantifiedIntegration() {
    }

    public static boolean register() {
        if (REGISTERED.get()) {
            return true;
        }

        try {
            boolean registered = QuantifiedAPI.register(MOD_ID, DISPLAY_NAME, detectVersion());
            REGISTERED.set(registered);
            return registered;
        } catch (Throwable throwable) {
            LOGGER.debug("Quantified API registration failed: {}", throwable.toString());
            REGISTERED.set(false);
            return false;
        }
    }

    public static boolean isAvailable() {
        try {
            Class.forName("org.admany.quantified.api.QuantifiedAPI");
            return register();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String detectVersion() {
        try {
            return ModList.get()
                    .getModContainerById(MOD_ID)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .filter(version -> !version.isBlank())
                    .orElse("unknown");
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    public static <T> CompletableFuture<T> submit(String taskName, Supplier<T> work) {
        if (!register()) {
            return CompletableFuture.supplyAsync(work);
        }

        try {
            return QuantifiedAPI.submit(taskName, work);
        } catch (Throwable throwable) {
            LOGGER.debug("Quantified submit failed for '{}', falling back locally: {}", taskName, throwable.toString());
            return CompletableFuture.supplyAsync(work);
        }
    }

    public static <T> T getCached(String cacheName, String key, Supplier<T> loader, Duration ttl, long maxSize) {
        if (!register()) {
            return loader.get();
        }

        try {
            return QuantifiedAPI.getCached(cacheName, key, loader, ttl, maxSize, false);
        } catch (Throwable throwable) {
            LOGGER.debug("Quantified cache miss/fallback for '{}:{}': {}", cacheName, key, throwable.toString());
            return loader.get();
        }
    }
}
