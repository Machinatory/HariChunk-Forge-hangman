package com.hari.harichunk.threading.worldgen.common;

import com.ibm.asyncutil.locks.AsyncLock;
import com.ibm.asyncutil.locks.AsyncNamedLock;
import net.minecraft.world.level.ChunkPos;

public interface IWorldGenLockable {

    AsyncLock getWorldGenSingleThreadedLock();

    AsyncNamedLock<ChunkPos> getWorldGenChunkLock();

}
