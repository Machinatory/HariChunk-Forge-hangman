package com.hari.harichunk.opts.dfc.common.ast.binary;

import com.hari.harichunk.opts.dfc.common.ast.AstNode;
import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.gen.BytecodeGen;
import com.hari.harichunk.opts.dfc.common.util.ArrayCache;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public class MaxShortNode extends AbstractBinaryNode {

    protected final double rightMax;

    public MaxShortNode(AstNode left, AstNode right, double rightMax) {
        super(left, right);
        this.rightMax = rightMax;
    }

    @Override
    protected AstNode newInstance(AstNode left, AstNode right) {
        return new MaxShortNode(left, right, this.rightMax);
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        double left = this.left.evalSingle(x, y, z, type);
        if (left >= this.rightMax) return left;
        return Math.max(left, this.right.evalSingle(x, y, z, type));
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type) {
        double[] res1 = new double[res.length];
        this.left.evalMulti(res, x, y, z, type);
        this.right.evalMulti(res1, x, y, z, type);
        for (int i = 0; i < res1.length; i++) {
            if (res[i] >= this.rightMax) continue;
            res[i] = Math.max(res[i], res1[i]);
        }
    }

    @Override
    public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String leftMethod = context.newSingleMethod(this.left);
        String rightMethod = context.newSingleMethod(this.right);
        context.callDelegateSingle(m, leftMethod);
        m.dup2();
        m.dconst(this.rightMax);
        m.cmpl(Type.DOUBLE_TYPE);
        Label skip = new Label();
        m.ifge(skip);
        m.pop2();
        context.callDelegateSingle(m, rightMethod);
        m.invokestatic(Type.getInternalName(Math.class), "max", Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE), false);
        m.mark(skip);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String leftMethod = context.newMultiMethod(this.left);
        String rightMethod = context.newMultiMethod(this.right);

        int res1 = localVarConsumer.createLocalVariable("res1", Type.getDescriptor(double[].class));

        m.load(6, InstructionAdapter.OBJECT_TYPE);
        m.load(1, InstructionAdapter.OBJECT_TYPE);
        m.arraylength();
        m.iconst(0);
        m.invokevirtual(Type.getInternalName(ArrayCache.class), "getDoubleArray", Type.getMethodDescriptor(Type.getType(double[].class), Type.INT_TYPE, Type.BOOLEAN_TYPE), false);
        m.store(res1, InstructionAdapter.OBJECT_TYPE);
        context.callDelegateMulti(m, leftMethod);

        Label skipRight = new Label();
        context.doCountedLoop(m, localVarConsumer, idx -> {
            m.load(1, InstructionAdapter.OBJECT_TYPE); m.load(idx, Type.INT_TYPE); m.dup2(); m.aload(Type.DOUBLE_TYPE);
            m.dconst(this.rightMax);
            m.cmpl(Type.DOUBLE_TYPE);
            m.ifge(skipRight);
        });

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.load(res1, InstructionAdapter.OBJECT_TYPE);
        m.load(2, InstructionAdapter.OBJECT_TYPE);
        m.load(3, InstructionAdapter.OBJECT_TYPE);
        m.load(4, InstructionAdapter.OBJECT_TYPE);
        m.load(5, InstructionAdapter.OBJECT_TYPE);
        m.load(6, InstructionAdapter.OBJECT_TYPE);
        m.invokevirtual(context.className, rightMethod, BytecodeGen.Context.MULTI_DESC, false);

        context.doCountedLoop(m, localVarConsumer, idx -> bytecodeGenMultiBody(m, idx, res1));

        m.mark(skipRight);

        m.load(6, InstructionAdapter.OBJECT_TYPE);
        m.load(res1, InstructionAdapter.OBJECT_TYPE);
        m.invokevirtual(Type.getInternalName(ArrayCache.class), "recycle", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(double[].class)), false);

        m.areturn(Type.VOID_TYPE);
    }

    @Override
    protected void bytecodeGenMultiBody(InstructionAdapter m, int idx, int res1) {
        m.load(1, InstructionAdapter.OBJECT_TYPE); m.load(idx, Type.INT_TYPE); m.dup2(); m.aload(Type.DOUBLE_TYPE);
        m.load(res1, InstructionAdapter.OBJECT_TYPE); m.load(idx, Type.INT_TYPE); m.aload(Type.DOUBLE_TYPE);
        m.invokestatic(Type.getInternalName(Math.class), "max", Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE), false);
        m.astore(Type.DOUBLE_TYPE);
    }
}
