package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.threading;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;

@Mixin(StrongholdPieces.StartPiece.class)
public class MixinStrongholdGeneratorStart {

    @Mutable
    @Shadow(remap = false) @Final public List<StructurePiece> f_229799_;  // pendingChildren

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.f_229799_ = Collections.synchronizedList(this.f_229799_);
    }

}
