package com.hari.harichunk.opts.dfc.common.ast.noise;

import com.hari.harichunk.opts.dfc.common.ast.AstNode;
import com.hari.harichunk.opts.dfc.common.ast.AstTransformer;
import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.ast.InvocationShim;
import com.hari.harichunk.opts.dfc.common.gen.BytecodeGen;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Objects;

public class DFTNoiseNode implements AstNode {

    private final DensityFunction.NoiseHolder noiseHolder;
    private final double xzScale;
    private final double yScale;

    public DFTNoiseNode(DensityFunction.NoiseHolder noiseHolder, double xzScale, double yScale) {
        this.noiseHolder = Objects.requireNonNull(noiseHolder);
        this.xzScale = xzScale;
        this.yScale = yScale;
    }

    public DensityFunction.NoiseHolder noiseHolder() {
        return this.noiseHolder;
    }

    public double xzScale() {
        return this.xzScale;
    }

    public double yScale() {
        return this.yScale;
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        return InvocationShim.invokeNoiseHolderSample(this.noiseHolder, x * xzScale, y * yScale, z * xzScale);
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        for (int i = 0; i < res.length; i++) {
            res[i] = InvocationShim.invokeNoiseHolderSample(this.noiseHolder, x[i] * xzScale, y[i] * yScale, z[i] * xzScale);
        }
    }

    @Override
    public AstNode[] getChildren() {
        return new AstNode[0];
    }

    @Override
    public AstNode transform(AstTransformer transformer) {
        return transformer.transform(this);
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(DensityFunction.NoiseHolder.class, this.noiseHolder);

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, noiseField, Type.getDescriptor(DensityFunction.NoiseHolder.class));

        m.load(1, Type.INT_TYPE);
        m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
        m.dconst(this.xzScale);
        m.mul(Type.DOUBLE_TYPE);

        m.load(2, Type.INT_TYPE);
        m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
        m.dconst(this.yScale);
        m.mul(Type.DOUBLE_TYPE);

        m.load(3, Type.INT_TYPE);
        m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
        m.dconst(this.xzScale);
        m.mul(Type.DOUBLE_TYPE);

        m.invokestatic(
                Type.getInternalName(InvocationShim.class),
                "invokeNoiseHolderSample",
                Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.getType(DensityFunction.NoiseHolder.class), Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE),
                false
        );
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(DensityFunction.NoiseHolder.class, this.noiseHolder);

        context.doCountedLoop(m, localVarConsumer, idx -> {
            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);

            {
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.getfield(context.className, noiseField, Type.getDescriptor(DensityFunction.NoiseHolder.class));

                m.load(2, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                m.dconst(this.xzScale);
                m.mul(Type.DOUBLE_TYPE);

                m.load(3, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                m.dconst(this.yScale);
                m.mul(Type.DOUBLE_TYPE);

                m.load(4, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
                m.dconst(this.xzScale);
                m.mul(Type.DOUBLE_TYPE);

                m.invokestatic(
                        Type.getInternalName(InvocationShim.class),
                        "invokeNoiseHolderSample",
                        Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.getType(DensityFunction.NoiseHolder.class), Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE),
                        false
                );
            }

            m.astore(Type.DOUBLE_TYPE);
        });

        m.areturn(Type.VOID_TYPE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFTNoiseNode that = (DFTNoiseNode) o;
        return Double.compare(xzScale, that.xzScale) == 0 && Double.compare(yScale, that.yScale) == 0 && Objects.equals(noiseHolder, that.noiseHolder);
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + noiseHolder.hashCode();
        result = 31 * result + Double.hashCode(xzScale);
        result = 31 * result + Double.hashCode(yScale);

        return result;
    }

    @Override
    public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFTNoiseNode that = (DFTNoiseNode) o;
        return Double.compare(xzScale, that.xzScale) == 0 && Double.compare(yScale, that.yScale) == 0;
    }

    @Override
    public int relaxedHashCode() {
        int result = 1;

        result = 31 * result + this.getClass().hashCode();
        result = 31 * result + Double.hashCode(xzScale);
        result = 31 * result + Double.hashCode(yScale);

        return result;
    }
}
