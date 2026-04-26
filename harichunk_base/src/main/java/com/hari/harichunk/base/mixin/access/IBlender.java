package com.hari.harichunk.base.mixin.access;

import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Blender.class)
public interface IBlender {

    @Accessor("HEIGHT_BLENDING_RANGE_CHUNKS")
    static int getBLENDING_CHUNK_DISTANCE_THRESHOLD() {
        throw new AbstractMethodError();
    }

}
