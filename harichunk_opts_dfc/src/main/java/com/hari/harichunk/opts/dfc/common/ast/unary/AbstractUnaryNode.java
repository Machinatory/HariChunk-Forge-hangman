package com.hari.harichunk.opts.dfc.common.ast.unary;

import com.hari.harichunk.opts.dfc.common.ast.AstTransformer;
import com.hari.harichunk.opts.dfc.common.ast.AstNode;
import com.hari.harichunk.opts.dfc.common.gen.BytecodeGen;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Objects;

public abstract class AbstractUnaryNode implements AstNode {
    protected final AstNode operand;
    public AbstractUnaryNode(AstNode operand) { this.operand = Objects.requireNonNull(operand); }
    @Override public AstNode[] getChildren() { return new AstNode[]{operand}; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(operand, ((AbstractUnaryNode) o).operand);
    }
    @Override public int hashCode() { return 31 * (31 + getClass().hashCode()) + operand.hashCode(); }
    @Override public boolean relaxedEquals(AstNode o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return operand.relaxedEquals(((AbstractUnaryNode) o).operand);
    }
    @Override public int relaxedHashCode() { return 31 * (31 + getClass().hashCode()) + operand.relaxedHashCode(); }
    protected abstract AstNode newInstance(AstNode operand);
    @Override public AstNode transform(AstTransformer transformer) {
        AstNode operand = this.operand.transform(transformer);
        if (operand == this.operand) return transformer.transform(this);
        else return transformer.transform(newInstance(operand));
    }
    @Override public void doBytecodeGenSingle(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        context.callDelegateSingle(m, context.newSingleMethod(this.operand));
    }
    @Override public void doBytecodeGenMulti(BytecodeGen.Context context, InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        context.callDelegateMulti(m, context.newMultiMethod(this.operand));
    }
}
