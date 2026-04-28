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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;

@Mixin(MineshaftPieces.MineShaftRoom.class)
public class MixinMineshaftGeneratorMineshaftRoom {

    @Mutable
    @Shadow(remap = false) @Final private List<BoundingBox> f_227900_;  // childEntranceBoxes

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.f_227900_ = Collections.synchronizedList(this.f_227900_);
    }

}
