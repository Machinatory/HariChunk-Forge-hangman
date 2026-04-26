package com.hari.harichunk.opts.dfc.common.vif;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Objects;

public class FunctionContextVanillaInterface implements DensityFunction.FunctionContext {

    private final int x;
    private final int y;
    private final int z;
    private final EvalType type;

    public FunctionContextVanillaInterface(int x, int y, int z, EvalType type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public int blockX() {
        return x;
    }

    @Override
    public int blockY() {
        return y;
    }

    @Override
    public int blockZ() {
        return z;
    }

    public EvalType getType() {
        return type;
    }

}
