package com.hari.harichunk.opts.scheduling.common;

import net.minecraft.world.level.LightLayer;

public interface DuckChunkHolder {

    void harichunk$queueLightSectionDirty(LightLayer lightType, int sectionY);

    boolean harichunk$shouldScheduleUndirty();

    boolean harichunk$undirtyLight();

}
