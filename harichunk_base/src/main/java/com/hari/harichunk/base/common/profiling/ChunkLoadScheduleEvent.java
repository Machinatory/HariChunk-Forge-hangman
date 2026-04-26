package com.hari.harichunk.base.common.profiling;

import jdk.jfr.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

@Name("minecraft.ChunkLoadSchedule")
@Label("Chunk Load Scheduling")
@Category({"Minecraft", "Chunk Loading"})
@StackTrace(false)
@Enabled(false)
public class    ChunkLoadScheduleEvent extends Event {

    public static final EventType TYPE = EventType.getEventType(ChunkLoadScheduleEvent.class);

    @Name("worldPosX")
    @Label("First Block X World Position")
    public final int worldPosX;
    @Name("worldPosZ")
    @Label("First Block Z World Position")
    public final int worldPosZ;
    @Name("chunkPosX")
    @Label("Chunk X Position")
    public final int chunkPosX;
    @Name("chunkPosZ")
    @Label("Chunk Z Position")
    public final int chunkPosZ;
    @Name("status")
    @Label("Status")
    public final String targetStatus;
    @Name("level")
    @Label("Level")
    public final String level;

    public ChunkLoadScheduleEvent(ChunkPos chunkPos, ResourceKey<Level> world, String targetStatus) {
        this.targetStatus = targetStatus;
        this.level = world.toString();
        this.chunkPosX = chunkPos.x;
        this.chunkPosZ = chunkPos.z;
        this.worldPosX = chunkPos.getMinBlockX();
        this.worldPosZ = chunkPos.getMinBlockZ();
    }
}
