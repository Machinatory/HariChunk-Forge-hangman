package com.hari.harichunk.base.mixin.access;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(CompoundTag.class)
public interface INbtCompound {

    @Invoker("entries")
    Map<String, Tag> invokeToMap();

}
