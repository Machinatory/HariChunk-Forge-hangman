package com.hari.harichunk.base.mixin.access;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentEntitySectionManager.class)
public interface IServerEntityManager {

    @Invoker("getAllChunksToSave")
    LongSet invokeGetLoadedChunks();

}
