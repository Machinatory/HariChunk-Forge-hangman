package com.hari.harichunk.notickvd.mixin;

import com.hari.harichunk.base.common.util.FilteringIterable;
import com.hari.harichunk.notickvd.common.IChunkTicketManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerChunkCache.class)
public class MixinServerChunkManager {

    @Shadow(remap = false) @Final private DistanceManager f_8327_;  // distanceManager

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;getTickingChunk()Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk includeAccessibleChunks(ChunkHolder instance) {
        return instance.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left().orElse(null);
    }

    @WrapOperation(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getAllEntities()Ljava/lang/Iterable;"))
    private Iterable<Entity> redirectIterateEntities(ServerLevel serverWorld, Operation<Iterable<Entity>> op) {
        final LongSet noTickOnlyChunks = ((IChunkTicketManager) this.f_8327_).getNoTickOnlyChunks();
        if (noTickOnlyChunks == null) return op.call(serverWorld);
        return new FilteringIterable<>(op.call(serverWorld), entity -> !noTickOnlyChunks.contains(entity.chunkPosition().toLong()));
    }

}
