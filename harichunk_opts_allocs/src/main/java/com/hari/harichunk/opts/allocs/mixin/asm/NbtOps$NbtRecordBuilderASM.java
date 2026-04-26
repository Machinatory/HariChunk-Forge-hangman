package com.hari.harichunk.opts.allocs.mixin.asm;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;

@Mixin(NbtOps.NbtRecordBuilder.class)
public class NbtOps$NbtRecordBuilderASM {

    @ModifyArg(method = "build(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/Tag;)Lcom/mojang/serialization/DataResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;<init>(Ljava/util/Map;)V"))
    private Map<String, Tag> test(Map<String, Tag> p_128333_) {
        return new Object2ObjectOpenHashMap<>(p_128333_);
    }
}
