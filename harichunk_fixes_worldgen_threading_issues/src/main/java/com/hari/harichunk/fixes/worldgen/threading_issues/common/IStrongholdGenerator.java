package com.hari.harichunk.fixes.worldgen.threading_issues.common;

import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;

public interface IStrongholdGenerator {

    ThreadLocal<Class<? extends StrongholdPieces.StrongholdPiece>> getActivePieceTypeThreadLocal();

    class Holder {
        @SuppressWarnings({"InstantiationOfUtilityClass", "ConstantConditions"})
        public static final IStrongholdGenerator INSTANCE = (IStrongholdGenerator) new StrongholdPieces();
    }

}
