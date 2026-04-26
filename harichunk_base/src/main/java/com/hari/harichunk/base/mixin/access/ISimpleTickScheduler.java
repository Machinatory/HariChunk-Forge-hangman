package com.hari.harichunk.base.mixin.access;

import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.SerializableTickContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ProtoChunkTicks.class)
public interface ISimpleTickScheduler<T> extends SerializableTickContainer<T> {
    @Accessor("ticks")
    List<SavedTick<T>> getScheduledTicks();
}
