package com.hari.harichunk.opts.dfc.common.ast;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.function.ToDoubleFunction;

public class InvocationShim {

    public static double invokeDensityFunctionCompute(DensityFunction densityFunction, DensityFunction.FunctionContext context) {
        return densityFunction.compute(context);
    }

    public static void invokeDensityFunctionFillArray(DensityFunction densityFunction, double[] densities, DensityFunction.ContextProvider provider) {
        densityFunction.fillArray(densities, provider);
    }

    public static double invokeMthClampedMap(double value, double oldStart, double oldEnd, double newStart, double newEnd) {
        return Mth.clampedMap(value, oldStart, oldEnd, newStart, newEnd);
    }

    public static double invokeNoiseHolderSample(DensityFunction.NoiseHolder noiseHolder, double x, double y, double z) {
        return noiseHolder.getValue(x, y, z);
    }

    public static float invokeMthLerp(float delta, float start, float end) {
        return Mth.lerp(delta, start, end);
    }

    public static int invokeFloor(double value) {
        return Mth.floor(value);
    }

    public static double invokeToDoubleFunctionApply(ToDoubleFunction<Double> function, double value) {
        return function.applyAsDouble(value);
    }

}
