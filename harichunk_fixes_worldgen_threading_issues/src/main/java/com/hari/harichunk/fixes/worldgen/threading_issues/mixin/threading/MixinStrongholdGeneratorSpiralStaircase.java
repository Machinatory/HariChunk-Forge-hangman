package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.threading;

import com.hari.harichunk.fixes.worldgen.threading_issues.common.IStrongholdGenerator;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StrongholdPieces.StairsDown.class)
public class MixinStrongholdGeneratorSpiralStaircase {

    @Redirect(method = "addChildren", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces;imposedPiece:Ljava/lang/Class;", opcode = Opcodes.PUTSTATIC))
    private void redirectGetActivePieceType(Class<? extends StrongholdPieces.StrongholdPiece> value) {
        IStrongholdGenerator.Holder.INSTANCE.getActivePieceTypeThreadLocal().set(value);
    }

}
