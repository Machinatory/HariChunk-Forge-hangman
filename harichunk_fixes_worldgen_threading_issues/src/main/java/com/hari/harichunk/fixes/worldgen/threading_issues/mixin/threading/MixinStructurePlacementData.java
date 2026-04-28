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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

@Mixin(StructurePlaceSettings.class)
public class MixinStructurePlacementData {

    @Mutable
    @Shadow(remap = false) @Final private List<StructureProcessor> f_74370_;  // processors

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.f_74370_ = Collections.synchronizedList(processors);
    }

}
