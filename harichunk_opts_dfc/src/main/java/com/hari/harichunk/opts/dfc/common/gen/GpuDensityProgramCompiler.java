package com.hari.harichunk.opts.dfc.common.gen;

import com.hari.harichunk.opts.dfc.common.ast.AstNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.AddNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.DivNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MaxNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MaxShortNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MinNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MinShortNode;
import com.hari.harichunk.opts.dfc.common.ast.binary.MulNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.CacheLikeNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.ConstantNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.hari.harichunk.opts.dfc.common.ast.misc.RootNode;
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
import com.hari.harichunk.opts.dfc.common.ast.unary.SqueezeNode;
import com.hari.harichunk.opts.dfc.common.ast.unary.SquareNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class GpuDensityProgramCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk/DFC-GPU-Compiler");

    private static final int STRIDE = 4;
    private static final int MAX_INSTRUCTIONS = 256;
    private static final int REGISTER_COUNT = 64;
    private static final int RESULT_REGISTER = 0;
    private static final int ZERO_REGISTER = 0;
    private static final int Y_REGISTER = 2;
    private static final int FIRST_TEMP_REGISTER = 4;

    private static final int OP_END = 0;
    private static final int OP_CONST = 1;
    private static final int OP_ADD = 10;
    private static final int OP_SUB = 11;
    private static final int OP_MUL = 12;
    private static final int OP_DIV = 13;
    private static final int OP_MIN = 14;
    private static final int OP_MAX = 15;
    private static final int OP_ABS = 16;
    private static final int OP_SQUARE = 17;
    private static final int OP_CUBE = 18;
    private static final int OP_NEG_MUL = 22;
    private static final int OP_GE = 23;
    private static final int OP_LT = 24;
    private static final int OP_AUX = 25;

    private static final ConcurrentHashMap<String, AtomicLong> UNSUPPORTED_NODES = new ConcurrentHashMap<>();

    private float[] encoded = new float[STRIDE * 64];
    private final List<AstNode> auxNodes = new ArrayList<>();
    private int instructionCount;
    private int nextRegister = FIRST_TEMP_REGISTER;
    private int meaningfulGpuOps;

    private GpuDensityProgramCompiler() {
    }

    static EncodedProgram compile(AstNode root) {
        try {
            GpuDensityProgramCompiler compiler = new GpuDensityProgramCompiler();
            int rootRegister = compiler.compileNode(root);
            if (rootRegister != RESULT_REGISTER) {
                compiler.emit(OP_ADD, RESULT_REGISTER, rootRegister, ZERO_REGISTER);
            }
            compiler.emit(OP_END, RESULT_REGISTER, 0, 0);
            if (!compiler.auxNodes.isEmpty() && compiler.meaningfulGpuOps <= 0) {
                recordUnsupported("aux-only");
                return null;
            }
            return new EncodedProgram(
                Arrays.copyOf(compiler.encoded, compiler.instructionCount * STRIDE),
                compiler.instructionCount,
                List.copyOf(compiler.auxNodes)
            );
        } catch (UnsupportedNode unsupportedNode) {
            recordUnsupported(unsupportedNode.nodeName);
            return null;
        }
    }

    private static void recordUnsupported(String nodeName) {
        long count = UNSUPPORTED_NODES
            .computeIfAbsent(nodeName, ignored -> new AtomicLong())
            .incrementAndGet();
        if (count == 1 || (count & 2047L) == 0L) {
            LOGGER.debug("DFC GPU program skipped for unsupported node {} (seen {} times)", nodeName, count);
        }
    }

    private int compileNode(AstNode node) {
        if (node instanceof RootNode || node instanceof CacheLikeNode) {
            return compileNode(node.getChildren()[0]);
        }
        if (node instanceof ConstantNode constantNode) {
            return constant((float) constantNode.getValue());
        }
        if (node instanceof YClampedGradientNode gradientNode) {
            return yClampedGradient(gradientNode);
        }
        if (node instanceof RangeChoiceNode rangeChoiceNode) {
            return rangeChoice(rangeChoiceNode);
        }
        if (node instanceof AddNode) {
            return binary(OP_ADD, node);
        }
        if (node instanceof MulNode) {
            return binary(OP_MUL, node);
        }
        if (node instanceof DivNode) {
            return binary(OP_DIV, node);
        }
        if (node instanceof MinNode || node instanceof MinShortNode) {
            return binary(OP_MIN, node);
        }
        if (node instanceof MaxNode || node instanceof MaxShortNode) {
            return binary(OP_MAX, node);
        }
        if (node instanceof AbsNode) {
            return unary(OP_ABS, node);
        }
        if (node instanceof SquareNode) {
            return unary(OP_SQUARE, node);
        }
        if (node instanceof CubeNode) {
            return unary(OP_CUBE, node);
        }
        if (node instanceof NegMulNode negMulNode) {
            int value = compileNode(negMulNode.getChildren()[0]);
            int factor = constant((float) negMulNode.getNegMul());
            int dst = allocateRegister();
            emit(OP_NEG_MUL, dst, value, factor);
            return dst;
        }
        if (node instanceof SqueezeNode) {
            return squeeze(node.getChildren()[0]);
        }
        if (isAuxLeafCandidate(node)) {
            return auxLeaf(node);
        }
        throw new UnsupportedNode(node.getClass().getSimpleName());
    }

    private static boolean isAuxLeafCandidate(AstNode node) {
        return node instanceof DFTNoiseNode
            || node instanceof DFTShiftNode
            || node instanceof DFTShiftANode
            || node instanceof DFTShiftBNode
            || node instanceof DFTWeirdScaledSamplerNode
            || node instanceof ShiftedNoiseNode
            || node instanceof SplineAstNode;
    }

    private int auxLeaf(AstNode node) {
        int auxIndex = auxNodes.indexOf(node);
        if (auxIndex < 0) {
            auxIndex = auxNodes.size();
            auxNodes.add(node);
        }
        int dst = allocateRegister();
        emit(OP_AUX, dst, auxIndex, 0);
        return dst;
    }

    private int yClampedGradient(YClampedGradientNode node) {
        int fromY = constant((float) node.getFromY());
        int numerator = allocateRegister();
        emit(OP_SUB, numerator, Y_REGISTER, fromY);

        int denominator = constant((float) (node.getToY() - node.getFromY()));
        int t = allocateRegister();
        emit(OP_DIV, t, numerator, denominator);

        int zero = constant(0.0f);
        int clampedLow = allocateRegister();
        emit(OP_MAX, clampedLow, t, zero);

        int one = constant(1.0f);
        int clamped = allocateRegister();
        emit(OP_MIN, clamped, clampedLow, one);

        int delta = constant((float) (node.getToValue() - node.getFromValue()));
        int scaled = allocateRegister();
        emit(OP_MUL, scaled, clamped, delta);

        int fromValue = constant((float) node.getFromValue());
        int dst = allocateRegister();
        emit(OP_ADD, dst, scaled, fromValue);
        return dst;
    }

    private int rangeChoice(RangeChoiceNode node) {
        AstNode[] children = node.getChildren();
        int input = compileNode(children[0]);
        int whenInRange = compileNode(children[1]);
        int whenOutOfRange = compileNode(children[2]);

        int lowerBound = constant((float) node.getMinInclusive());
        int geLower = allocateRegister();
        emit(OP_GE, geLower, input, lowerBound);

        int upperBound = constant((float) node.getMaxExclusive());
        int ltUpper = allocateRegister();
        emit(OP_LT, ltUpper, input, upperBound);

        int inRange = allocateRegister();
        emit(OP_MUL, inRange, geLower, ltUpper);

        int selectedIn = allocateRegister();
        emit(OP_MUL, selectedIn, whenInRange, inRange);

        int one = constant(1.0f);
        int outRange = allocateRegister();
        emit(OP_SUB, outRange, one, inRange);

        int selectedOut = allocateRegister();
        emit(OP_MUL, selectedOut, whenOutOfRange, outRange);

        int dst = allocateRegister();
        emit(OP_ADD, dst, selectedIn, selectedOut);
        return dst;
    }

    private int squeeze(AstNode operand) {
        int value = compileNode(operand);

        int min = constant(-1.0f);
        int lower = allocateRegister();
        emit(OP_MAX, lower, value, min);

        int max = constant(1.0f);
        int clamped = allocateRegister();
        emit(OP_MIN, clamped, lower, max);

        int two = constant(2.0f);
        int half = allocateRegister();
        emit(OP_DIV, half, clamped, two);

        int cube = allocateRegister();
        emit(OP_CUBE, cube, clamped, 0);

        int twentyFour = constant(24.0f);
        int cubeScaled = allocateRegister();
        emit(OP_DIV, cubeScaled, cube, twentyFour);

        int dst = allocateRegister();
        emit(OP_SUB, dst, half, cubeScaled);
        return dst;
    }

    private int binary(int opcode, AstNode node) {
        AstNode[] children = node.getChildren();
        int left = compileNode(children[0]);
        int right = compileNode(children[1]);
        int dst = allocateRegister();
        emit(opcode, dst, left, right);
        return dst;
    }

    private int unary(int opcode, AstNode node) {
        int value = compileNode(node.getChildren()[0]);
        int dst = allocateRegister();
        emit(opcode, dst, value, 0);
        return dst;
    }

    private int constant(float value) {
        int dst = allocateRegister();
        emit(OP_CONST, dst, value, 0.0f);
        return dst;
    }

    private int allocateRegister() {
        if (nextRegister >= REGISTER_COUNT) {
            throw new UnsupportedNode("register-limit");
        }
        return nextRegister++;
    }

    private void emit(int opcode, int dst, int a, int b) {
        emit(opcode, (float) dst, (float) a, (float) b);
    }

    private void emit(int opcode, int dst, float a, float b) {
        emit(opcode, (float) dst, a, b);
    }

    private void emit(int opcode, float a, float b, float c) {
        if (instructionCount >= MAX_INSTRUCTIONS) {
            throw new UnsupportedNode("instruction-limit");
        }
        int offset = instructionCount * STRIDE;
        if (encoded.length < offset + STRIDE) {
            encoded = Arrays.copyOf(encoded, encoded.length * 2);
        }
        encoded[offset] = opcode;
        encoded[offset + 1] = a;
        encoded[offset + 2] = b;
        encoded[offset + 3] = c;
        instructionCount++;
        if (opcode != OP_END && opcode != OP_CONST && opcode != OP_AUX) {
            meaningfulGpuOps++;
        }
    }

    record EncodedProgram(float[] encoded, int instructionCount, List<AstNode> auxNodes) {
        EncodedProgram(float[] encoded, int instructionCount) {
            this(encoded, instructionCount, List.of());
        }

        boolean hasAuxNodes() {
            return !auxNodes.isEmpty();
        }
    }

    private static final class UnsupportedNode extends RuntimeException {
        private final String nodeName;

        private UnsupportedNode(String nodeName) {
            super(nodeName, null, false, false);
            this.nodeName = nodeName;
        }
    }
}
