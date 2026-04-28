package com.hari.harichunk.fixes.chunkio.threading_issues.mixin;

import com.hari.harichunk.fixes.chunkio.threading_issues.common.SynchronizedCodec;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructurePoolElement.class)
public class MixinStructurePoolElement {

    @Mutable
    @Shadow(remap = false) @Final public static Codec<StructurePoolElement> f_210468_;  // CODEC

    @Dynamic
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void onCLInit(CallbackInfo info) {
        f_210468_ = new SynchronizedCodec<>(f_210468_);
    }

}
