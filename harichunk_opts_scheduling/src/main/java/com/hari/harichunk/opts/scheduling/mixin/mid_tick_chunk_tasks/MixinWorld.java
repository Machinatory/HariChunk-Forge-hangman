package com.hari.harichunk.opts.scheduling.mixin.mid_tick_chunk_tasks;

import com.hari.harichunk.opts.scheduling.common.ServerMidTickTask;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class MixinWorld {

    @Shadow(remap = false) @Nullable public abstract MinecraftServer m_7654_();

    @Shadow(remap = false) @Final public boolean f_46443_; // isClientSide

    @Inject(method = "guardEntityTick", at = @At("TAIL"))
    private void onPostTickEntity(CallbackInfo ci) {
        final MinecraftServer server = this.m_7654_();
        if (!this.f_46443_ && server != null) {
            ((ServerMidTickTask) server).executeTasksMidTick((ServerLevel) (Object) this);
        }
    }

}
