package com.hari.harichunk.base.mixin.access;

import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionStorage.class)
public interface ISerializingRegionBasedStorage {

    @Accessor
    IOWorker getWorker();

}
