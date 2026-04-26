package com.hari.harichunk.notickvd.common;

import it.unimi.dsi.fastutil.longs.LongSet;

public interface IChunkTicketManager {

    LongSet getNoTickOnlyChunks();

    int getNoTickPendingTicketUpdates();

}
