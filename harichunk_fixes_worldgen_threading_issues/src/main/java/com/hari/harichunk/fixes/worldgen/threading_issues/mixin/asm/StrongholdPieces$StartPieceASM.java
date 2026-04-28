package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.asm;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(StrongholdPieces.StartPiece.class)
public class StrongholdPieces$StartPieceASM {

    @Shadow(remap = false)
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE)
    public StrongholdPieces.PieceWeight f_229797_;  // previousPiece

    @Shadow(remap = false)
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE)
    @Nullable
    public StrongholdPieces.PortalRoom f_229798_;  // portalRoomPiece
}
