package org.admany.quantifiedintegration.opencl;

import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
 * <p>Fix: A daemon thread continuously scans for any QAPI worker thread and
 * corrects its context classloader. This is robust against timing issues,
 * lazy pool creation, and local variable captures of old pool references.
 * Additionally, we replace ParallelScheduler's SHARED ForkJoinPool field
 * so new pools also get the correct ThreadFactory.</p>
 */
public final class QAPIThreadClassloaderFix {

    private static final String PARALLEL_SCHEDULER_CLASS =
            "org.admany.quantified.core.common.parallel.executor.ParallelScheduler";

    private static final String[] QAPI_THREAD_PREFIXES = {
            "quantified-parallel-",
            "quantified-fg-",
            "quantified-bg-",
            "quantified-hk",
    };

    private static final long SCAN_INTERVAL_MS = 500;
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

        try {
            replaceParallelSchedulerPool(logger);
        } catch (Throwable t) {
            logger.debug("[HariChunk] Could not replace ParallelScheduler pool: {}", t.toString());
        }

        Thread scanner = new Thread(() -> {
            int fixedCount = 0;
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

                Thread[] threads = new Thread[Thread.activeCount() * 2];
                int count = Thread.enumerate(threads);
                for (int i = 0; i < count; i++) {
                    Thread t = threads[i];
                    if (t == null || !t.isAlive()) {
                        continue;
                    }
                    if (!isQapiThread(t)) {
                        continue;
                    }
                    ClassLoader cl = t.getContextClassLoader();
                    if (cl == forgeClassLoader) {
                        continue;
                    }
                    t.setContextClassLoader(forgeClassLoader);
                    fixedCount++;
                    if (fixedCount <= 5) {
                        logger.info("[HariChunk] Fixed classloader on thread: {}", t.getName());
                    }
                }
            }
        }, "harichunk-qapi-cl-fix");
        scanner.setDaemon(true);
        scanner.setPriority(Thread.MIN_PRIORITY);
        scanner.start();

        logger.info("[HariChunk] QAPI thread classloader fix active (scanner started)");
    }

    private static void replaceParallelSchedulerPool(Logger logger) throws Exception {
        Class<?> schedulerClass = Class.forName(PARALLEL_SCHEDULER_CLASS);
        Method executorMethod = schedulerClass.getMethod("executor");
        Object poolObj = executorMethod.invoke(null);

        if (!(poolObj instanceof ForkJoinPool oldPool)) {
            return;
        }

        int parallelism = oldPool.getParallelism();
        AtomicInteger counter = new AtomicInteger(0);

        ForkJoinPool newPool = new ForkJoinPool(
                parallelism,
                pool -> {
                    ForkJoinWorkerThread worker =
                            ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    worker.setName("quantified-parallel-" + counter.incrementAndGet());
                    worker.setDaemon(true);
                    worker.setPriority(Thread.NORM_PRIORITY);
                    worker.setContextClassLoader(forgeClassLoader);
                    return worker;
                },
                null,
                true
        );

        oldPool.shutdown();
        try {
            oldPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Field sharedField = schedulerClass.getDeclaredField("SHARED");
        sharedField.setAccessible(true);
        sharedField.set(null, newPool);

        logger.info("[HariChunk] Replaced ParallelScheduler ForkJoinPool ({} threads)",
                parallelism);
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
