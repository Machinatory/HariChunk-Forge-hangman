package com.hari.harichunk.base.common.scheduler;

import com.hari.harichunk.base.common.structs.DynamicPriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.world.level.ChunkPos;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulingManager {

    public static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
    private final DynamicPriorityQueue<ScheduledTask> queue = new DynamicPriorityQueue<>(MAX_LEVEL + 1);
    private final Long2ReferenceOpenHashMap<ObjectArraySet<ScheduledTask>> pos2Tasks = new Long2ReferenceOpenHashMap<>();
    private final Long2IntOpenHashMap prioritiesFromLevel = new Long2IntOpenHashMap();
    private final NeighborLockingManager neighborLockingManager = new NeighborLockingManager();
    private final AtomicInteger scheduledCount = new AtomicInteger(0);
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private ChunkPos currentSyncLoad = null;

    private final Executor executor;
    private final int maxScheduled;

    {
        prioritiesFromLevel.defaultReturnValue(MAX_LEVEL);
    }

    public SchedulingManager(Executor executor, int maxScheduled) {
        this.executor = executor;
        this.maxScheduled = maxScheduled;
    }

    public void enqueue(ScheduledTask task) {
        this.executor.execute(() -> {
            if (task.isAsync() || isDagSchedulerActive()) {
                scheduleDirect(task);
            } else {
                queue.enqueue(task, prioritiesFromLevel.get(task.centerPos()));
                pos2Tasks.computeIfAbsent(task.centerPos(), unused -> new ObjectArraySet<>()).add(task);
                scheduleExecution();
            }
        });
    }

    public void updatePriorityFromLevel(long pos, int level) {
        this.executor.execute(() -> {
            if (prioritiesFromLevel.get(pos) == level) return;
            if (level < MAX_LEVEL) {
                prioritiesFromLevel.put(pos, level);
            } else {
                prioritiesFromLevel.remove(pos);
            }
            updatePriorityInternal(pos);
        });
    }

    private void updatePriorityInternal(long pos) {
        int fromLevel = prioritiesFromLevel.get(pos);
        int fromSyncLoad;
        if (currentSyncLoad != null) {
            final int chebyshevDistance = chebyshev(new ChunkPos(pos), currentSyncLoad);
            if (chebyshevDistance <= 8) {
                fromSyncLoad = chebyshevDistance;
//                System.out.println("dist for chunk [%d,%d] is %d".formatted(currentSyncLoad.x, currentSyncLoad.z, chebyshevDistance));
            } else {
                fromSyncLoad = MAX_LEVEL;
            }
        } else {
            fromSyncLoad = MAX_LEVEL;
        }
        int priority = Math.min(fromLevel, fromSyncLoad);
        final ObjectArraySet<ScheduledTask> locks = this.pos2Tasks.get(pos);
        if (locks != null) {
            for (ScheduledTask lock : locks) {
                queue.changePriority(lock, priority);
            }
        }
    }

    public void setCurrentSyncLoad(ChunkPos pos) {
        executor.execute(() -> {
            if (this.currentSyncLoad != null) {
                final ChunkPos lastSyncLoad = this.currentSyncLoad;
                this.currentSyncLoad = null;
                updateSyncLoadInternal(lastSyncLoad);
            }
            if (pos != null) {
                this.currentSyncLoad = pos;
                updateSyncLoadInternal(pos);
            }
        });
    }

    public NeighborLockingManager getNeighborLockingManager() {
        return this.neighborLockingManager;
    }

    public Executor getExecutor() {
        return executor;
    }

    private void updateSyncLoadInternal(ChunkPos pos) {
        long startTime = System.nanoTime();
        for (int xOff = -8; xOff <= 8; xOff++) {
            for (int zOff = -8; zOff <= 8; zOff++) {
                updatePriorityInternal(ChunkPos.asLong(pos.x + xOff, pos.z + zOff));
            }
        }
        long endTime = System.nanoTime();
    }

    private void scheduleExecution() {
        boolean dagActive = isDagSchedulerActive();
        if ((dagActive || scheduledCount.get() < maxScheduled) && scheduled.compareAndSet(false, true)) {
            this.executor.execute(() -> {
                boolean dagMode = isDagSchedulerActive();
                while ((dagMode || scheduledCount.get() < maxScheduled) && scheduleExecutionInternal(dagMode)) {
                    if (!dagMode) {
                        scheduledCount.incrementAndGet();
                    }
                }
                scheduled.set(false);
                if (queue.size() > 0 && (isDagSchedulerActive() || scheduledCount.get() < maxScheduled)) {
                    scheduleExecution();
                }
            });
        }
    }

    private boolean scheduleExecutionInternal(boolean dagMode) {
        final ScheduledTask task = queue.dequeue();
        if (task != null) {
            this.pos2Tasks.get(task.centerPos()).remove(task);
            runPos2TasksMaintenance(task.centerPos());
            boolean scheduled1 = schedule0(task, dagMode
                    ? this::scheduleExecution
                    : () -> {
                        scheduledCount.decrementAndGet();
                        scheduleExecution();
                    });
            if (scheduled1) return true;
        }
        return false;
    }

    private void scheduleDirect(ScheduledTask task) {
        schedule0(task, isDagSchedulerActive() ? this::scheduleExecution : () -> {
        });
    }

    private boolean schedule0(ScheduledTask task, Runnable postAction) {
        if (task.tryPrepare()) {
            task.runTask(postAction);
            return true;
        }
        return false;
    }

    private static int chebyshev(ChunkPos a, ChunkPos b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.z - b.z));
    }

    private static int chebyshev(long a, long b) {
        return Math.max(Math.abs(ChunkPos.getX(a) - ChunkPos.getX(b)), Math.abs(ChunkPos.getZ(a) - ChunkPos.getZ(b)));
    }

    private void runPos2TasksMaintenance(long pos) {
        final ObjectArraySet<ScheduledTask> locks = this.pos2Tasks.get(pos);
        if (locks != null && locks.isEmpty()) {
            this.pos2Tasks.remove(pos);
        }
    }

    private static boolean isDagSchedulerActive() {
        Method method = DagSchedulerHolder.IS_USING_QUANTIFIED_METHOD;
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static final class DagSchedulerHolder {
        private static final Method IS_USING_QUANTIFIED_METHOD = findIsUsingQuantifiedMethod();

        private static Method findIsUsingQuantifiedMethod() {
            try {
                Class<?> schedulerClass = Class.forName("org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler");
                return schedulerClass.getMethod("isUsingQuantified");
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
    }

}
