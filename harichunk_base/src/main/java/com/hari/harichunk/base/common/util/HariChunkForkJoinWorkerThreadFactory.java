package com.hari.harichunk.base.common.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class HariChunkForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private final AtomicLong serial = new AtomicLong(0);
    private final String groupName;
    private final String namePattern;
    private final int priority;

    private final ExecutorService threadCreator;
    private final ThreadGroup threadGroup;

    public HariChunkForkJoinWorkerThreadFactory(String groupName, String namePattern, int priority) {
        this.groupName = groupName;
        this.namePattern = namePattern;
        this.priority = priority;

        this.threadGroup = new ThreadGroup(this.groupName);
        this.threadCreator = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat(String.format("%s daemon", this.groupName))
                        .setPriority(Thread.NORM_PRIORITY - 1)
                        .setDaemon(true)
                        .setThreadFactory(r -> new Thread(this.threadGroup, r))
                        .build()
        );
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return CFUtil.join(CompletableFuture.supplyAsync(() -> {
            final HariChunkForkJoinWorkerThread newThread = new HariChunkForkJoinWorkerThread(pool);
            newThread.setName(String.format(namePattern, serial.incrementAndGet()));
            newThread.setPriority(priority);
            newThread.setDaemon(true);
            return newThread;
        }, threadCreator));
    }

    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    public static class HariChunkForkJoinWorkerThread extends ForkJoinWorkerThread {

        /**
         * Creates a ForkJoinWorkerThread operating in the given pool.
         *
         * @param pool the pool this thread works in
         * @throws NullPointerException if pool is null
         */
        protected HariChunkForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool);
        }

    }
}
