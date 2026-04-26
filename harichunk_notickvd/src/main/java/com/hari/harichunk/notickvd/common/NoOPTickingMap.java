package com.hari.harichunk.notickvd.common;

import com.hari.harichunk.base.mixin.access.IThreadedAnvilChunkStorage;
import java.util.function.LongPredicate;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.TickingTracker;
import net.minecraft.world.level.ChunkPos;

public class NoOPTickingMap extends TickingTracker {

    private ChunkMap tacs = null;

    public void setTACS(ChunkMap tacs) {
        this.tacs = tacs;
    }

    @Override
    public void addTicket(long l, Ticket<?> chunkTicket) {
    }

    @Override
    public void removeTicket(long l, Ticket<?> chunkTicket) {
    }

    @Override
    public <T> void addTicket(TicketType<T> chunkTicketType, ChunkPos chunkPos, int i, T object) {
    }

    @Override
    public <T> void removeTicket(TicketType<T> chunkTicketType, ChunkPos chunkPos, int i, T object) {
    }

    @Override
    public void replacePlayerTicketsLevel(int i) {
    }

    @Override
    protected int getLevelFromSource(long id) {
        return super.getLevelFromSource(id);
    }

    @Override
    public int getLevel(ChunkPos chunkPos) {
        return getLevel(chunkPos.toLong());
    }

    @Override
    protected int getLevel(long id) {
        if (tacs != null) {
            final ChunkHolder holder = ((IThreadedAnvilChunkStorage) tacs).getCurrentChunkHolders().get(id);
            return holder != null ? holder.getTicketLevel() : ChunkLevel.MAX_LEVEL + 1;
        } else {
            return 0;
        }
    }

    @Override
    protected void setLevel(long id, int level) {
    }

    @Override
    public void runAllUpdates() {
    }

    @Override
    public String getTicketDebugString(long l) {
        return "no-op";
    }

    @Override
    protected boolean isSource(long id) {
        return super.isSource(id);
    }

    @Override
    protected void checkNeighborsAfterUpdate(long id, int level, boolean decrease) {
        super.checkNeighborsAfterUpdate(id, level, decrease);
    }

    @Override
    protected int getComputedLevel(long id, long excludedId, int maxLevel) {
        return super.getComputedLevel(id, excludedId, maxLevel);
    }

    @Override
    protected int computeLevelFromNeighbor(long sourceId, long targetId, int level) {
        return super.computeLevelFromNeighbor(sourceId, targetId, level);
    }

    @Override
    public void update(long chunkPos, int distance, boolean decrease) {
    }

    @Override
    protected void removeFromQueue(long id) {
    }

    @Override
    public void removeIf(LongPredicate predicate) {
    }

    @Override
    protected void checkNode(long id) {
    }

    @Override
    protected void checkEdge(long sourceId, long id, int level, boolean decrease) {
    }

    @Override
    public int getQueueSize() {
        return 0;
    }
}
