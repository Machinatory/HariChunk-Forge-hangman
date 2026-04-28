package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.threading;

import com.hari.harichunk.fixes.worldgen.threading_issues.common.XPieceDataExtension;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;

@Mixin(NetherFortressPieces.StartPiece.class)
public class MixinNetherFortressGeneratorStart {

    @Shadow(remap = false) public List<NetherFortressPieces.PieceWeight> f_228508_;  // availableBridgePieces
    @Shadow(remap = false) public List<NetherFortressPieces.PieceWeight> f_228509_;  // availableCastlePieces

    @Redirect(method = "<init>(Lnet/minecraft/util/RandomSource;II)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/NetherFortressPieces$PieceWeight;placeCount:I", opcode = Opcodes.PUTFIELD), require = 2)
    private void redirectSetPieceDataGeneratedCount(NetherFortressPieces.PieceWeight pieceData, int value) {
        if (value == 0) {
            ((XPieceDataExtension) pieceData).harichunk$getGeneratedCountThreadLocal().remove();
        } else {
            ((XPieceDataExtension) pieceData).harichunk$getGeneratedCountThreadLocal().set(value);
        }
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        this.f_228508_ = Collections.synchronizedList(this.f_228508_);
        this.f_228509_ = Collections.synchronizedList(this.f_228509_);
    }
}
