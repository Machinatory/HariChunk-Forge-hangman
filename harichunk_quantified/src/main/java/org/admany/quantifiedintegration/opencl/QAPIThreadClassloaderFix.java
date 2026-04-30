package org.admany.quantifiedintegration.opencl;

import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fixes QAPI thread classloaders so that parallel workers can load Forge classes.
 *
 * <p>Problem: QAPI creates multiple thread pools (ParallelScheduler ForkJoinPool,
 * PriorityScheduler foreground/background pools, housekeeper) with ThreadFactories
 * that do NOT set the context classloader. Worker threads inherit the system
 * AppClassLoader instead of Forge's classloader. When these threads load Minecraft
 * classes that extend Forge classes (e.g. CapabilityProvider), it fails with
 * ClassNotFoundException, causing world loading to freeze.</p>
 *
 * <p>Fix: An immediate scan corrects any QAPI threads that already exist, then a
 * daemon thread continuously scans for new QAPI worker threads and patches their
 * classloader. This is robust against timing issues, lazy pool creation, and
 * dynamic thread pool resizing.</p>
 */
public final class QAPIThreadClassloaderFix {

    private static final String[] QAPI_THREAD_PREFIXES = {
            "quantified-parallel-",
            "quantified-fg-",
            "quantified-bg-",
            "quantified-hk",
    };

    private static final long SCAN_INTERVAL_MS = 100;
    private static final AtomicBoolean active = new AtomicBoolean(false);
    private static volatile ClassLoader forgeClassLoader;

    private QAPIThreadClassloaderFix() {
    }

    public static void fix(Logger logger) {
        if (!active.compareAndSet(false, true)) {
            return;
        }

        forgeClassLoader = Thread.currentThread().getContextClassLoader();
        if (forgeClassLoader == null) {
            logger.debug("[HariChunk] Thread classloader fix: no Forge classloader available");
            active.set(false);
            return;
        }

        int immediate = scanAndFix();
        if (immediate > 0) {
            logger.info("[HariChunk] QAPI classloader fix: {} threads patched immediately", immediate);
        }

        Thread scanner = new Thread(() -> {
            int totalFixed = immediate;
            while (active.get()) {
                try {
                    Thread.sleep(SCAN_INTERVAL_MS);
                } catch (InterruptedException e) {
                    if (!active.get()) {
                        break;
                    }
                    Thread.currentThread().interrupt();
                    continue;
                }

                int fixed = scanAndFix();
                if (fixed > 0) {
                    totalFixed += fixed;
                    if (totalFixed <= 20) {
                        logger.info("[HariChunk] QAPI classloader fix: {} new threads patched (total: {})",
                                fixed, totalFixed);
                    }
                }
            }
        }, "harichunk-qapi-cl-fix");
        scanner.setDaemon(true);
        scanner.setPriority(Thread.NORM_PRIORITY + 1);
        scanner.start();

        logger.info("[HariChunk] QAPI thread classloader fix active (scan interval: {}ms)", SCAN_INTERVAL_MS);
    }

    private static int scanAndFix() {
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        int count = Thread.enumerate(threads);
        int fixed = 0;
        for (int i = 0; i < count; i++) {
            Thread t = threads[i];
            if (t == null || !t.isAlive()) {
                continue;
            }
            if (!isQapiThread(t)) {
                continue;
            }
            if (t.getContextClassLoader() == forgeClassLoader) {
                continue;
            }
            t.setContextClassLoader(forgeClassLoader);
            fixed++;
        }
        return fixed;
    }

    private static boolean isQapiThread(Thread t) {
        String name = t.getName();
        for (String prefix : QAPI_THREAD_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
