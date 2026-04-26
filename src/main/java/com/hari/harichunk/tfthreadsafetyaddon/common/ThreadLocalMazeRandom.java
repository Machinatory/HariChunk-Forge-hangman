package com.hari.harichunk.tfthreadsafetyaddon.common;

import net.minecraft.util.RandomSource;

public final class ThreadLocalMazeRandom {
    private static final ThreadLocal<RandomSource> MAZE_RANDOM = new ThreadLocal<>();

    private ThreadLocalMazeRandom() {
    }

    public static void set(RandomSource random) {
        MAZE_RANDOM.set(random);
    }

    public static void clear() {
        MAZE_RANDOM.remove();
    }

    public static RandomSource get() {
        return MAZE_RANDOM.get();
    }
}
