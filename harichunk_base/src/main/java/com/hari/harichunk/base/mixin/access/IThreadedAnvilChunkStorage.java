package com.hari.harichunk.base.mixin.access;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface IThreadedAnvilChunkStorage {

    @Accessor("level")
    ServerLevel getWorld();

    @Invoker("promoteChunkMap")
    boolean invokeUpdateHolderMap();

    @Accessor("visibleChunkMap")
    Long2ObjectLinkedOpenHashMap<ChunkHolder> getChunkHolders();

    @Accessor("updatingChunkMap")
    Long2ObjectLinkedOpenHashMap<ChunkHolder> getCurrentChunkHolders();

    @Invoker("saveChunkIfNeeded")
    boolean invokeSave(ChunkHolder chunkHolder);

    @Invoker
    void invokeReleaseLightTicket(ChunkPos pos);

    @Accessor
    BlockableEventLoop<Runnable> getMainThreadExecutor();

}
