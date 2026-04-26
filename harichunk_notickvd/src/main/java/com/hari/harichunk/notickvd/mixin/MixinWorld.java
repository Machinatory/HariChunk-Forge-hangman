package com.hari.harichunk.notickvd.mixin;

import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = Level.class, priority = 1101)
public class MixinWorld {

    @Shadow @Final public boolean isClientSide;

    @Dynamic
    @ModifyArg(method = {"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", "markAndNotifyBlock"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/FullChunkStatus;isOrAfter(Lnet/minecraft/server/level/FullChunkStatus;)Z"))
    private FullChunkStatus modifyLeastStatus(FullChunkStatus levelType) {
        return levelType.ordinal() > FullChunkStatus.FULL.ordinal() ? FullChunkStatus.FULL : levelType;
    }

}
