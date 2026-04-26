package com.hari.harichunk.base.mixin.access;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockEntity.class)
public interface IBlockEntity {

    @Invoker("saveAdditional")
    void invokeWriteNbt(CompoundTag nbt);

}
