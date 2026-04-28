package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.asm;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MineshaftPieces.MineShaftCorridor.class)
public class MineshaftPieces$MineShaftCorridorASM {

    @Shadow(remap = false)
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE)
    private boolean f_227728_;  // hasPlacedSpider
}
