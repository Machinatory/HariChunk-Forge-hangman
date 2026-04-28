package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.asm;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WoodlandMansionPieces.PlacementData.class)
public class WoodlandMansionPieces$PlacementDataASM {

    @Shadow(remap = false)
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE)
    public Rotation f_230138_;  // rotation

    @Shadow(remap = false)
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE)
    public BlockPos f_230139_;  // position

    @Shadow(remap = false)
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE)
    public String f_230140_;  // wallType
}
