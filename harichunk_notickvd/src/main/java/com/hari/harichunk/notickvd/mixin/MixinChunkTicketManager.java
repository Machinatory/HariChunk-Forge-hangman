package com.hari.harichunk.notickvd.mixin;

import com.hari.harichunk.notickvd.common.IChunkTicketManager;
import com.hari.harichunk.notickvd.common.NoOPTickingMap;
import com.hari.harichunk.notickvd.common.NoTickSystem;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TickingTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DistanceManager.class)
public class MixinChunkTicketManager implements IChunkTicketManager {

    @Shadow private long ticketTickCounter;
    @Mutable
    @Shadow @Final private TickingTracker tickingTicketsTracker;
    @Shadow @Final private DistanceManager.PlayerTicketTracker playerTicketManager;

    @Unique
    private NoTickSystem noTickSystem;

    @Unique
    private long lastNoTickSystemTick = -1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.noTickSystem = new NoTickSystem((DistanceManager) (Object) this);
        this.tickingTicketsTracker = new NoOPTickingMap();
    }

    @Inject(method = "addPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/DistanceManager$FixedPlayerDistanceChunkTracker;update(JIZ)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onHandleChunkEnter(SectionPos pos, ServerPlayer player, CallbackInfo ci) {
        this.noTickSystem.addPlayerSource(pos.chunk());
    }

    @Inject(method = "removePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/DistanceManager$FixedPlayerDistanceChunkTracker;update(JIZ)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onHandleChunkLeave(SectionPos pos, ServerPlayer player, CallbackInfo ci) {
        this.noTickSystem.removePlayerSource(pos.chunk());
    }

    @Inject(method = "purgeStaleTickets", at = @At("RETURN"))
    private void onPurge(CallbackInfo ci) {
        this.noTickSystem.runPurge(this.ticketTickCounter);
    }

    @Inject(method = "runAllUpdates", at = @At("HEAD"))
    private void beforeTick(ChunkMap chunkStorage, CallbackInfoReturnable<Boolean> cir) {
        this.noTickSystem.beforeTicketTicks();
    }

    @Inject(method = "runAllUpdates", at = @At("RETURN"))
    private void onTick(ChunkMap chunkStorage, CallbackInfoReturnable<Boolean> cir) {
        if (this.tickingTicketsTracker instanceof NoOPTickingMap map) {
            map.setTACS(chunkStorage);
        }
        this.noTickSystem.tickScheduler();
        this.noTickSystem.afterTicketTicks();
        if (this.lastNoTickSystemTick != this.ticketTickCounter) {
            this.noTickSystem.tick(chunkStorage);
            this.lastNoTickSystemTick = this.ticketTickCounter;
        }
    }

    @Inject(method = "addTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("RETURN"))
    private void onAddTicket(long position, Ticket<?> ticket, CallbackInfo ci) {
//        if (ticket.getType() != ChunkTicketType.UNKNOWN) System.err.printf("Added ticket (%s) at %s\n", ticket, new ChunkPos(position));
        this.noTickSystem.onTicketAdded(position, ticket);
    }

    @Inject(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("RETURN"))
    private void onRemoveTicket(long pos, Ticket<?> ticket, CallbackInfo ci) {
//        if (ticket.getType() != ChunkTicketType.UNKNOWN) System.err.printf("Removed ticket (%s) at %s\n", ticket, new ChunkPos(pos));
        this.noTickSystem.onTicketRemoved(pos, ticket);
    }

    /**
     * @author Hari
     * @reason remap setSimulationDistance to the normal one
     */
    @Overwrite
    public void updateSimulationDistance(int i) {
        this.playerTicketManager.updateViewDistance(i);
    }

    /**
     * @author Hari
     * @reason remap setWatchDistance to no-tick one
     */
    @Overwrite
    public void updatePlayerTickets(int viewDistance) {
        this.noTickSystem.setNoTickViewDistance(viewDistance);
    }

    @Override
    @Unique
    public LongSet getNoTickOnlyChunks() {
        return this.noTickSystem.getNoTickOnlyChunksSnapshot();
    }

    @Override
    @Unique
    public int getNoTickPendingTicketUpdates() {
        return this.noTickSystem.getPendingNoTickTicketUpdatesCount();
    }
}
