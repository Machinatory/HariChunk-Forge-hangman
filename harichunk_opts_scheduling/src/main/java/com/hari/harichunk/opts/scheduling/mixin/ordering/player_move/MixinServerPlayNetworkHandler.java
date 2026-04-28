package com.hari.harichunk.opts.scheduling.mixin.ordering.player_move;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerPlayNetworkHandler {

    @Shadow(remap = false) public ServerPlayer f_9743_; // player

//    @Inject(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", shift = At.Shift.BEFORE))
//    private void movePacketBeforePlayerMove(CallbackInfo ci) {
//        this.player.getWorld().getChunkManager().updatePosition(this.player);
//    }
//
//    @Redirect(
//            method = "onPlayerMove",
//            slice = @Slice(
//                    from = @At(
//                            value = "INVOKE",
//                            target = "Lnet/minecraft/server/network/ServerPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
//                    )
//            ),
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/server/world/ServerChunkManager;updatePosition(Lnet/minecraft/server/network/ServerPlayerEntity;)V"
//            )
//    )
//    private void movePacketAvoidDoubleUpdate(ServerChunkManager instance, ServerPlayerEntity player) {
//        // no-op
//    }

}
