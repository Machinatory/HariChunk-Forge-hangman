package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.threading;

import com.hari.harichunk.fixes.worldgen.threading_issues.common.XPieceDataExtension;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StrongholdPieces.PieceWeight.class)
public class MixinStrongholdGeneratorPieceData implements XPieceDataExtension {

    @Unique
    private final ThreadLocal<Integer> generatedCountThreadLocal = ThreadLocal.withInitial(() -> 0);

    @Dynamic
    @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces$PieceWeight;placeCount:I", opcode = Opcodes.GETFIELD), require = 2)
    private int redirectGetGeneratedCount(StrongholdPieces.PieceWeight pieceData) {
        return this.generatedCountThreadLocal.get();
    }

    @SuppressWarnings("MixinAnnotationTarget")
    @Dynamic
    @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces$PieceWeight;placeCount:I", opcode = Opcodes.PUTFIELD), require = 0, expect = 0)
    private void redirectSetGeneratedCount(StrongholdPieces.PieceWeight pieceData, int value) {
        if (value == 0) {
            generatedCountThreadLocal.remove();
        } else {
            this.generatedCountThreadLocal.set(value);
        }
    }

    @Override
    public ThreadLocal<Integer> harichunk$getGeneratedCountThreadLocal() {
        return this.generatedCountThreadLocal;
    }

}
