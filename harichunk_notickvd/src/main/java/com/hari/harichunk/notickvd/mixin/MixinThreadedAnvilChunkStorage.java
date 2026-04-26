package com.hari.harichunk.notickvd.mixin;

import com.hari.harichunk.base.mixin.access.IServerChunkManager;
import com.hari.harichunk.notickvd.common.Config;
import com.hari.harichunk.notickvd.common.IChunkHolder;
import com.hari.harichunk.notickvd.common.IChunkTicketManager;
import com.hari.harichunk.notickvd.common.NoTickChunkSendingInterceptor;
import com.mojang.datafixers.util.Either;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow protected abstract void playerLoadedChunk(ServerPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject, LevelChunk chunk);

    @Shadow public abstract List<ServerPlayer> getPlayers(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge);

    @Shadow @Final private BlockableEventLoop<Runnable> mainThreadExecutor;

    @ModifyArg(method = "setViewDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"), index = 2)
    private int modifyMaxVD(int max) {
        return 251;
    }

    @Redirect(method = "updateChunkTracking", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;getTickingChunk()Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk redirectSendWatchPacketsGetWorldChunk(ChunkHolder chunkHolder) {
        return ((IChunkHolder) chunkHolder).getAccessibleChunk();
    }

    @Inject(method = "prepareAccessibleChunk", at = @At("RETURN"))
    private void onMakeChunkAccessible(ChunkHolder chunkHolder, CallbackInfoReturnable<CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>>> cir) {
        cir.getReturnValue().thenAccept(either -> either.left().ifPresent(worldChunk -> {
            MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject = new MutableObject<>();
            this.getPlayers(worldChunk.getPos(), false).forEach((serverPlayerEntity) -> {
                if (NoTickChunkSendingInterceptor.onChunkSending(serverPlayerEntity, worldChunk.getPos().toLong())) {
                    if (Config.compatibilityMode) {
                        this.mainThreadExecutor.tell(() -> this.playerLoadedChunk(serverPlayerEntity, mutableObject, worldChunk));
                    } else {
                        this.playerLoadedChunk(serverPlayerEntity, mutableObject, worldChunk);
                    }
                }
            });
        }));
    }

    // private synthetic method_17243(Lorg/apache/commons/lang3/mutable/MutableObject;Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/server/network/ServerPlayerEntity;)V
    /**
     * @author Hari
     * @reason dont send chunks twice
     */
    @Dynamic
    @Overwrite(remap = false)
    private void m_214908_(MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject, LevelChunk worldChunk, ServerPlayer player) {
        if (Config.ensureChunkCorrectness && NoTickChunkSendingInterceptor.onChunkSending(player, worldChunk.getPos().toLong()))
            this.playerLoadedChunk(player, mutableObject, worldChunk);
    }

    // private static synthetic method_20582(Lnet/minecraft/world/chunk/Chunk;)Z
    @Dynamic
    @Inject(method = "m_203087_", at = @At("RETURN"), cancellable = true) // TODO lambda expression of the 1st filter "chunk instanceof ReadOnlyChunk || chunk instanceof WorldChunk"
    private static void onSaveFilter1(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (true) return;
        if (chunk instanceof LevelChunk worldChunk) {
            final ServerLevel serverWorld = (ServerLevel) worldChunk.getLevel();
            final IServerChunkManager serverChunkManager = (IServerChunkManager) serverWorld.getChunkSource();
            final IChunkTicketManager ticketManager =
                    (IChunkTicketManager) serverChunkManager.getTicketManager();
            cir.setReturnValue(cir.getReturnValueZ() && !ticketManager.getNoTickOnlyChunks().contains(chunk.getPos().toLong()));
        }
    }

}
