package com.hari.harichunk.rewrites.chunk_serializer.mixin;

import com.hari.harichunk.rewrites.chunk_serializer.common.ChunkStatusAccessor;
import com.hari.harichunk.rewrites.chunk_serializer.common.NbtWriter;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin implements ChunkStatusAccessor {
    @Shadow(remap = false) public abstract String toString();  // toString

    @Unique
    private byte[] idBytes;

    @Override
    public byte[] getIdBytes() {
        return this.idBytes != null ? this.idBytes : (this.idBytes = NbtWriter.getStringBytes(this.toString()));
    }
}
