package com.hari.harichunk.opts.scheduling.common;

import java.util.concurrent.Executor;

public interface IThreadedAnvilChunkStorage {

    Executor getMainInvokingExecutor();

}
