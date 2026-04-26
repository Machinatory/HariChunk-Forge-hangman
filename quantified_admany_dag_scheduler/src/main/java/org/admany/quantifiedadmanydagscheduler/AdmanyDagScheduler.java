/*
 * HariChunk Admany Quantified DAG Scheduler by Admany / BlackRift Studios.
 * Licensed strictly under BRSSLA V1.5.
 * This is original BlackRift Studios code and is not related to C2ME in any way.
 *
 * This source may be viewed and contributed to through approved project channels.
 * Modification, copying, redistribution, reuse, or derivative use outside those
 * approved contribution flows is strictly not allowed. C2ME may not copy, modify,
 * reuse, redistribute, or derive from this code.
 */
package org.admany.quantifiedadmanydagscheduler;

import com.hari.harichunk.base.common.GlobalExecutors;
import net.minecraft.server.level.ServerLevel;
import org.admany.quantified.api.QuantifiedAPI;
import org.admany.quantified.api.builders.QuantifiedTaskBuilder;
import org.admany.quantified.api.graph.QuantifiedTaskGraph;
import org.admany.quantifiedintegration.QuantifiedIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

public final class AdmanyDagScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk Admany DAG Scheduler");

    private static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("harichunk.admanyDagScheduler.enabled", "true"));
    private static final int BACKLOG_WARN_THRESHOLD = Integer.getInteger(
            "harichunk.admanyDagScheduler.backlogWarnThreshold", 512);
    private static final long AVAILABILITY_RECHECK_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final Duration TASK_TIMEOUT = Duration.ofSeconds(Long.getLong(
            "harichunk.admanyDagScheduler.taskTimeoutSeconds", 45L));

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final AtomicInteger PENDING = new AtomicInteger();
    private static final AtomicInteger SUBMITTED = new AtomicInteger();
    private static final AtomicInteger COMPLETED = new AtomicInteger();
    private static final AtomicInteger FAILED = new AtomicInteger();
    private static final AtomicInteger QAPI_SUBMITTED = new AtomicInteger();
    private static final AtomicInteger FALLBACK_SUBMITTED = new AtomicInteger();
    private static final AtomicLong LAST_RECHECK = new AtomicLong();
    private static final AtomicLong LAST_BACKLOG_LOG = new AtomicLong();

    private static volatile boolean quantifiedAvailable;
    private static volatile String unavailableReason = "not initialized";

    private AdmanyDagScheduler() {
    }

    public static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        if (!ENABLED) {
            unavailableReason = "disabled by harichunk.admanyDagScheduler.enabled=false";
            LOGGER.info("Admany DAG scheduler disabled");
            return;
        }

        refreshQuantifiedAvailability(true);
        LOGGER.info("Admany DAG scheduler ready [qapi={}, reason={}]", quantifiedAvailable, unavailableReason);
    }

    public static Executor executor(String description, double priority, boolean foreground) {
        return command -> submitChunkTask(description, priority, foreground, () -> {
            command.run();
            return null;
        });
    }

    public static CompletableFuture<Void> submitAsync(Runnable work, String description) {
        return submitChunkTask(description, 0.5, false, () -> {
            work.run();
            return null;
        });
    }

    public static <T> CompletableFuture<T> submitChunkTask(String description,
                                                           double priority,
                                                           boolean foreground,
                                                           Supplier<T> work) {
        initialize();
        SUBMITTED.incrementAndGet();
        PENDING.incrementAndGet();

        CompletableFuture<T> future;
        if (ENABLED && isQuantifiedAvailable()) {
            try {
                future = submitGraphNode(description, priority, foreground, work);
                QAPI_SUBMITTED.incrementAndGet();
            } catch (Throwable throwable) {
                quantifiedAvailable = false;
                unavailableReason = throwable.toString();
                LOGGER.debug("QAPI DAG submit failed for '{}', using HariChunk fallback: {}", description, throwable.toString());
                future = submitFallback(work);
            }
        } else {
            future = submitFallback(work);
        }

        return future.whenComplete((result, throwable) -> {
            PENDING.decrementAndGet();
            if (throwable == null) {
                COMPLETED.incrementAndGet();
            } else {
                FAILED.incrementAndGet();
            }
        });
    }

    public static <T> CompletableFuture<T> submitChunkFutureTask(String description,
                                                                 double priority,
                                                                 boolean foreground,
                                                                 Supplier<CompletableFuture<T>> work) {
        return submitChunkTask(description, priority, foreground, work).thenCompose(Function.identity());
    }

    public static void executeMidTickTasks(ServerLevel level) {
        if (!ENABLED) {
            return;
        }
        refreshQuantifiedAvailability(false);

        int pending = PENDING.get();
        long now = System.nanoTime();
        if (pending >= BACKLOG_WARN_THRESHOLD &&
                now - LAST_BACKLOG_LOG.get() >= TimeUnit.SECONDS.toNanos(5) &&
                LAST_BACKLOG_LOG.compareAndSet(LAST_BACKLOG_LOG.get(), now)) {
            LOGGER.debug("High Admany DAG backlog in {}: {} pending [{}]",
                    level.dimension().location(), pending, getStatsString());
        }
    }

    public static boolean isUsingQuantified() {
        return ENABLED && quantifiedAvailable;
    }

    public static int getPendingTaskCount() {
        return PENDING.get();
    }

    public static String getStatsString() {
        refreshQuantifiedAvailability(false);
        return "AdmanyDag[qapi=%s,pending=%d,submitted=%d,qapiSubmitted=%d,fallback=%d,completed=%d,failed=%d,reason=%s]"
                .formatted(quantifiedAvailable, PENDING.get(), SUBMITTED.get(), QAPI_SUBMITTED.get(),
                        FALLBACK_SUBMITTED.get(), COMPLETED.get(), FAILED.get(), unavailableReason);
    }

    private static <T> CompletableFuture<T> submitGraphNode(String description,
                                                            double priority,
                                                            boolean foreground,
                                                            Supplier<T> work) {
        if (!QuantifiedIntegration.register()) {
            throw new IllegalStateException("Quantified API registration failed");
        }

        String scope = normalize(description);
        QuantifiedTaskGraph.Builder graph = QuantifiedAPI.graph(QuantifiedIntegration.MOD_ID, "admany-dag-" + scope);
        graph.localityKey(scope);

        QuantifiedTaskGraph.NodeHandle<T> node = graph.node("run", work)
                .priority(mapPriority(priority, foreground))
                .threadSafe(true)
                .timeout(TASK_TIMEOUT)
                .batchKey(scope)
                .localityKey(scope);

        return QuantifiedAPI.submitGraph(graph, node);
    }

    private static <T> CompletableFuture<T> submitFallback(Supplier<T> work) {
        FALLBACK_SUBMITTED.incrementAndGet();
        return CompletableFuture.supplyAsync(work, GlobalExecutors.executor);
    }

    private static boolean isQuantifiedAvailable() {
        refreshQuantifiedAvailability(false);
        return quantifiedAvailable;
    }

    private static void refreshQuantifiedAvailability(boolean force) {
        long now = System.nanoTime();
        long previous = LAST_RECHECK.get();
        if (!force && now - previous < AVAILABILITY_RECHECK_NANOS) {
            return;
        }
        if (!LAST_RECHECK.compareAndSet(previous, now) && !force) {
            return;
        }

        try {
            quantifiedAvailable = QuantifiedIntegration.isAvailable();
            unavailableReason = quantifiedAvailable ? "ready" : "Quantified API unavailable";
        } catch (Throwable throwable) {
            quantifiedAvailable = false;
            unavailableReason = throwable.toString();
        }
    }

    private static QuantifiedTaskBuilder.Priority mapPriority(double priority, boolean foreground) {
        if (priority >= 0.95) {
            return QuantifiedTaskBuilder.Priority.CRITICAL;
        }
        if (foreground || priority >= 0.65) {
            return QuantifiedTaskBuilder.Priority.FOREGROUND;
        }
        if (priority <= 0.25) {
            return QuantifiedTaskBuilder.Priority.BACKGROUND;
        }
        return QuantifiedTaskBuilder.Priority.AUTO;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "chunk-task";
        }
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._:-]+", "-");
        if (normalized.length() > 80) {
            return normalized.substring(0, 80);
        }
        return normalized;
    }
}
