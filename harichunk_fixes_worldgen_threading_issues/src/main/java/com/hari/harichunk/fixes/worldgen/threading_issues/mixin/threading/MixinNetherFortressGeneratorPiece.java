package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.threading;

import com.hari.harichunk.fixes.worldgen.threading_issues.common.XPieceDataExtension;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NetherFortressPieces.NetherBridgePiece.class)
public class MixinNetherFortressGeneratorPiece {

    @Redirect(method = {"updatePieceWeight", "generatePiece"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/NetherFortressPieces$PieceWeight;placeCount:I", opcode = Opcodes.GETFIELD))
    private int redirectGetPieceDataGeneratedCount(NetherFortressPieces.PieceWeight pieceData) {
        return ((XPieceDataExtension) pieceData).harichunk$getGeneratedCountThreadLocal().get();
    }

    @Redirect(method = "generatePiece", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/NetherFortressPieces$PieceWeight;placeCount:I", opcode = Opcodes.PUTFIELD))
    private void redirectIncrementPieceDataGeneratedCount(NetherFortressPieces.PieceWeight pieceData, int value) { // TODO Check when updating minecraft version
        ((XPieceDataExtension) pieceData).harichunk$getGeneratedCountThreadLocal().set(value);
    }

}
