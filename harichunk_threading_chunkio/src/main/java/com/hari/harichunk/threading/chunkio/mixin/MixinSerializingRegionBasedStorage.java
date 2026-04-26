package com.hari.harichunk.threading.chunkio.mixin;

import com.hari.harichunk.threading.chunkio.common.ISerializingRegionBasedStorage;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SectionStorage.class)
public abstract class MixinSerializingRegionBasedStorage implements ISerializingRegionBasedStorage {

    @Shadow
    protected abstract <T> void readColumn(ChunkPos pos, DynamicOps<T> dynamicOps, @Nullable T data);

    @Shadow @Final private RegistryAccess registryAccess;

    @Override
    public void update(ChunkPos pos, CompoundTag tag) {
        this.readColumn(pos, RegistryOps.create(NbtOps.INSTANCE, this.registryAccess), tag);
    }

}
