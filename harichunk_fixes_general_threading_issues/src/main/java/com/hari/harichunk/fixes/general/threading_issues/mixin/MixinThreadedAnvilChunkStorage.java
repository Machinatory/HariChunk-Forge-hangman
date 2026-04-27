package com.hari.harichunk.fixes.general.threading_issues.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkMap.class)
public class MixinThreadedAnvilChunkStorage {

    @Shadow(remap = false) @Final private BlockableEventLoop<Runnable> f_140135_; // mainThreadExecutor

    @Shadow(remap = false) @Final ServerLevel f_140133_; // level

    @Redirect(method = "schedule", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap$DistanceManager;addTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;)V"))
    private <T> void redirectAddLightTicket(ChunkMap.DistanceManager ticketManager, TicketType<T> type, ChunkPos pos, int level, T argument) {
        if (this.f_140133_.getServer().getRunningThread() != Thread.currentThread()) {
            this.f_140135_.execute(() -> ticketManager.addTicket(type, pos, level, argument));
        } else {
            ticketManager.addTicket(type, pos, level, argument);
        }
    }

    @Redirect(method = "scheduleChunkGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getProfiler()Lnet/minecraft/util/profiling/ProfilerFiller;"))
    private ProfilerFiller removeProfilerUsage(ServerLevel instance) {
        return InactiveProfiler.INSTANCE;
    }

}
