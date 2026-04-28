package com.hari.harichunk.notickvd.mixin;

import com.hari.harichunk.notickvd.common.IChunkHolder;
import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder implements IChunkHolder {

    @Shadow(remap = false) public abstract CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> m_140082_();  // getFullChunkFuture

    @Shadow(remap = false) @Nullable public abstract LevelChunk m_140085_();  // getTickingChunk

    @Shadow(remap = false) public abstract ChunkPos m_140092_();  // getPos

    @Unique
    @Override
    public LevelChunk getAccessibleChunk() {
        final Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> either = this.m_140082_().getNow(null);
        return either == null ? null : either.left().orElseGet(this::getTickingChunk);
    }

    @Redirect(method = {"blockChanged", "sectionLightChanged"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;m_140085_()Lnet/minecraft/world/level/chunk/LevelChunk;"), require = 2)
    private LevelChunk redirectWorldChunk(ChunkHolder chunkHolder) {
        return this.getAccessibleChunk();
    }

}
