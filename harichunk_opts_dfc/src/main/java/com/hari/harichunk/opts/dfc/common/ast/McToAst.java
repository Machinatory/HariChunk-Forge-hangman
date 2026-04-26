package com.hari.harichunk.opts.dfc.common.ast;

import com.hari.harichunk.opts.dfc.common.ast.binary.AddNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MaxNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MaxShortNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MinNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MinShortNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MulNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.CacheLikeNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.ConstantNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.DelegateNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.hari.harichunk.opts.dfc.common.ast.noise.DFTNoiseNode;
import com.hari.harichunk.opts.dfc.common.ast.noise.DFTShiftANode;
import com.hari.harichunk.opts.dfc.common.ast.noise.DFTShiftBNode;
import com.hari.harichunk.opts.dfc.common.ast.noise.DFTShiftNode;
import com.hari.harichunk.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.hari.harichunk.opts.dfc.common.ast.noise.ShiftedNoiseNode;
import com.hari.harichunk.opts.dfc.common.ast.spline.SplineAstNode;
import com.hari.harichunk.opts.dfc.common.ast.unary.AbsNode;
import com.hari.harichunk.opts.dfc.common.ast.unary.CubeNode;
import com.hari.harichunk.opts.dfc.common.ast.unary.NegMulNode;
import com.hari.harichunk.opts.dfc.common.ast.unary.SquareNode;
import com.hari.harichunk.opts.dfc.common.ast.unary.SqueezeNode;
import com.hari.harichunk.opts.dfc.common.ducks.IFastCacheLike;
import com.hari.harichunk.opts.dfc.common.ducks.IEqualityOverriding;
import com.hari.harichunk.opts.dfc.common.gen.BytecodeGen;
import com.hari.harichunk.opts.dfc.common.gen.CompiledDensityFunction;
import com.hari.harichunk.opts.dfc.common.vif.AstVanillaInterface;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;

import java.util.Objects;

public class McToAst {

//    private static final ConcurrentHashMap<Class<?>, LongAdder> delegateStatistics = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static AstNode toAst(DensityFunction df) {
        Objects.requireNonNull(df);

        if (df instanceof AstVanillaInterface) {
            return ((AstVanillaInterface) df).getAstNode();
        } else if (df instanceof NoiseChunk.BlendAlpha) {
            return new ConstantNode(1.0);
        } else if (df instanceof NoiseChunk.BlendOffset) {
            return new ConstantNode(0.0);
        } else if (df instanceof DensityFunctions.BlendAlpha) {
            return new ConstantNode(1.0);
        } else if (df instanceof DensityFunctions.BlendOffset) {
            return new ConstantNode(0.0);
        } else if (df instanceof DensityFunctions.TwoArgumentSimpleFunction) {
            DensityFunctions.TwoArgumentSimpleFunction f = (DensityFunctions.TwoArgumentSimpleFunction) df;
            switch (f.type()) {
                case ADD:
                    return new AddNode(toAst(f.argument1()), toAst(f.argument2()));
                case MUL:
                    return new MulNode(toAst(f.argument1()), toAst(f.argument2()));
                case MIN: {
                    double rightMin = f.argument2().minValue();
                    if (f.argument1().minValue() < rightMin) {
                        return new MinShortNode(toAst(f.argument1()), toAst(f.argument2()), rightMin);
                    } else {
                        return new MinNode(toAst(f.argument1()), toAst(f.argument2()));
                    }
                }
                case MAX: {
                    double rightMax = f.argument2().maxValue();
                    if (f.argument1().maxValue() > rightMax) {
                        return new MaxShortNode(toAst(f.argument1()), toAst(f.argument2()), rightMax);
                    } else {
                        return new MaxNode(toAst(f.argument1()), toAst(f.argument2()));
                    }
                }
                default:
                    break;
            }
        } else if (df instanceof DensityFunctions.BlendDensity) {
            return toAst(((DensityFunctions.BlendDensity) df).input());
        } else if (df instanceof DensityFunctions.Clamp) {
            DensityFunctions.Clamp f = (DensityFunctions.Clamp) df;
            return new MinNode(
                    new ConstantNode(f.maxValue()),
                    new MaxNode(
                            new ConstantNode(f.minValue()),
                            toAst(f.input())
                    )
            );
        } else if (df instanceof DensityFunctions.Constant) {
            return new ConstantNode(((DensityFunctions.Constant) df).value());
        } else if (df instanceof DensityFunctions.HolderHolder) {
            return toAst(((DensityFunctions.HolderHolder) df).function().value());
        } else if (df instanceof DensityFunctions.Mapped) {
            DensityFunctions.Mapped f = (DensityFunctions.Mapped) df;
            switch (f.type()) {
                case ABS:
                    return new AbsNode(toAst(f.input()));
                case SQUARE:
                    return new SquareNode(toAst(f.input()));
                case CUBE:
                    return new CubeNode(toAst(f.input()));
                case HALF_NEGATIVE:
                    return new NegMulNode(toAst(f.input()), 0.5);
                case QUARTER_NEGATIVE:
                    return new NegMulNode(toAst(f.input()), 0.25);
                case SQUEEZE:
                    return new SqueezeNode(toAst(f.input()));
                default:
                    break;
            }
            // Note: INVERT does not exist in 1.20.1 DensityFunctions.Mapped.Type
        } else if (df instanceof DensityFunctions.RangeChoice) {
            DensityFunctions.RangeChoice f = (DensityFunctions.RangeChoice) df;
            return new RangeChoiceNode(
                    toAst(f.input()),
                    f.minInclusive(),
                    f.maxExclusive(),
                    toAst(f.whenInRange()),
                    toAst(f.whenOutOfRange())
            );
        } else if (df instanceof IFastCacheLike) {
            IFastCacheLike f = (IFastCacheLike) df;
            return new CacheLikeNode(f, toAst(f.harichunk$getDelegate()));
        } else if (df instanceof DensityFunctions.Marker) {
            DensityFunctions.Marker f = (DensityFunctions.Marker) df;
            DensityFunctions.Marker wrapping = new DensityFunctions.Marker(
                    f.type(),
                    new CompiledDensityFunction(
                            BytecodeGen.compile0(toAst(f.wrapped())),
                            null
                    )
            );
            ((IEqualityOverriding) (Object) wrapping).harichunk$overrideEquality(f);
            return new DelegateNode(wrapping);
        } else if (df instanceof DensityFunctions.ShiftedNoise) {
            DensityFunctions.ShiftedNoise f = (DensityFunctions.ShiftedNoise) df;
            return new ShiftedNoiseNode(
                    toAst(f.shiftX()),
                    toAst(f.shiftY()),
                    toAst(f.shiftZ()),
                    f.xzScale(),
                    f.yScale(),
                    f.noise()
            );
        } else if (df instanceof DensityFunctions.Noise) {
            DensityFunctions.Noise f = (DensityFunctions.Noise) df;
            return new DFTNoiseNode(f.noise(), f.xzScale(), f.yScale());
        } else if (df instanceof DensityFunctions.Shift) {
            return new DFTShiftNode(((DensityFunctions.Shift) df).offsetNoise());
        } else if (df instanceof DensityFunctions.ShiftA) {
            return new DFTShiftANode(((DensityFunctions.ShiftA) df).offsetNoise());
        } else if (df instanceof DensityFunctions.ShiftB) {
            return new DFTShiftBNode(((DensityFunctions.ShiftB) df).offsetNoise());
        } else if (df instanceof DensityFunctions.YClampedGradient) {
            DensityFunctions.YClampedGradient f = (DensityFunctions.YClampedGradient) df;
            return new YClampedGradientNode(f.fromY(), f.toY(), f.fromValue(), f.toValue());
        } else if (df instanceof DensityFunctions.WeirdScaledSampler) {
            DensityFunctions.WeirdScaledSampler f = (DensityFunctions.WeirdScaledSampler) df;
            return new DFTWeirdScaledSamplerNode(
                    toAst(f.input()),
                    f.noise(),
                    f.rarityValueMapper()
            );
        } else if (df instanceof DensityFunctions.Spline) {
            return new SplineAstNode(((DensityFunctions.Spline) df).spline());
        }
        // Note: DensityFunctionTypes.FindTopSurface does not exist in MC 1.20.1

        // Fallthrough to delegate
        return new DelegateNode(df);
    }

    public static DensityFunction wrapVanilla(DensityFunction densityFunction) {
        return new AstVanillaInterface(McToAst.toAst(densityFunction), densityFunction);
    }

}
