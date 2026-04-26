package com.hari.harichunk.opts.dfc.common.ast.unary;

import com.hari.harichunk.opts.dfc.common.ast.AstNode;
import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.gen.BytecodeGen;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public class AbsNode extends AbstractUnaryNode {

    public AbsNode(AstNode operand) {
        super(operand);
    }

    @Override
    protected AstNode newInstance(AstNode operand) {
        return new AbsNode(operand);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        return Math.abs(this.operand.evalSingle(x, y, z, type));
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        this.operand.evalMulti(res, x, y, z, type);
        for (int i = 0; i < res.length; i++) {
            res[i] = Math.abs(res[i]);
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        super.doBytecodeGenSingle(context, m, localVarConsumer);
        Label nonNeg = new Label();
        m.dup2();
        m.dconst(0.0);
        m.cmpl(Type.DOUBLE_TYPE);
        m.ifge(nonNeg);
        m.neg(Type.DOUBLE_TYPE);
        m.visitLabel(nonNeg);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        super.doBytecodeGenMulti(context, m, localVarConsumer);
        context.doCountedLoop(m, localVarConsumer, idx -> {
            Label nonNeg = new Label();
            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);
            m.dup2();
            m.aload(Type.DOUBLE_TYPE);
            m.dup2();
            m.dconst(0.0);
            m.cmpl(Type.DOUBLE_TYPE);
            m.ifge(nonNeg);
            m.neg(Type.DOUBLE_TYPE);
            m.visitLabel(nonNeg);
            m.astore(Type.DOUBLE_TYPE);
        });
        m.areturn(Type.VOID_TYPE);
    }
}
