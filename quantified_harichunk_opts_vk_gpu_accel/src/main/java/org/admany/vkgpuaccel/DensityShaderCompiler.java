/*
 * HariChunk Quantified Vulkan GPU Acceleration by Admany / BlackRift Studios.
 * Licensed strictly under BRSSLA V1.5.
 * This is original BlackRift Studios code and is not related to C2ME in any way.
 *
 * This source may be viewed and contributed to through approved project channels.
 * Modification, copying, redistribution, reuse, or derivative use outside those
 * approved contribution flows is strictly not allowed. C2ME may not copy, modify,
 * reuse, redistribute, or derive from this code.
 */
package org.admany.vkgpuaccel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DensityShaderCompiler {

    private static final int HC_OP_END     = 0;
    private static final int HC_OP_CONST   = 1;
    private static final int HC_OP_ADD     = 10;
    private static final int HC_OP_SUB     = 11;
    private static final int HC_OP_MUL     = 12;
    private static final int HC_OP_DIV     = 13;
    private static final int HC_OP_MIN     = 14;
    private static final int HC_OP_MAX     = 15;
    private static final int HC_OP_ABS     = 16;
    private static final int HC_OP_SQUARE  = 17;
    private static final int HC_OP_CUBE    = 18;
    private static final int HC_OP_NEG_MUL = 22;
    private static final int HC_OP_GE      = 23;
    private static final int HC_OP_LT      = 24;
    private static final int HC_OP_AUX     = 25;

    private static final int SPIRV_MAGIC    = 0x07230203;
    private static final int SPIRV_VERSION  = 0x00010500;
    private static final int SPIRV_GENERATOR = 0x48434100;

    private static final int SP_CAPABILITY        = 17;
    private static final int SP_EXT_INST_IMPORT   = 11;
    private static final int SP_EXT_INST          = 12;
    private static final int SP_MEMORY_MODEL      = 14;
    private static final int SP_ENTRY_POINT       = 15;
    private static final int SP_EXECUTION_MODE    = 16;
    private static final int SP_DECORATE          = 71;
    private static final int SP_MEMBER_DECORATE   = 72;
    private static final int SP_TYPE_VOID         = 19;
    private static final int SP_TYPE_BOOL         = 20;
    private static final int SP_TYPE_INT          = 21;
    private static final int SP_TYPE_FLOAT        = 22;
    private static final int SP_TYPE_VECTOR       = 23;
    private static final int SP_TYPE_RUNTIME_ARRAY = 29;
    private static final int SP_TYPE_STRUCT       = 30;
    private static final int SP_TYPE_POINTER      = 32;
    private static final int SP_TYPE_FUNCTION     = 33;
    private static final int SP_CONSTANT          = 43;
    private static final int SP_FUNCTION          = 54;
    private static final int SP_FUNCTION_END      = 56;
    private static final int SP_VARIABLE          = 59;
    private static final int SP_LOAD              = 61;
    private static final int SP_STORE             = 62;
    private static final int SP_ACCESS_CHAIN      = 65;
    private static final int SP_COMPOSITE_EXTRACT = 81;
    private static final int SP_BITCAST           = 124;
    private static final int SP_F_NEGATE          = 127;
    private static final int SP_I_ADD             = 128;
    private static final int SP_F_ADD             = 129;
    private static final int SP_I_MUL             = 132;
    private static final int SP_F_MUL             = 133;
    private static final int SP_F_DIV             = 136;
    private static final int SP_F_SUB             = 131;
    private static final int SP_F_ORD_LESS_THAN   = 184;
    private static final int SP_F_ORD_GREATER_THAN = 186;
    private static final int SP_F_ORD_GREATER_THAN_EQUAL = 190;
    private static final int SP_S_GREATER_THAN_EQUAL = 175;
    private static final int SP_SELECT            = 169;
    private static final int SP_LABEL             = 248;
    private static final int SP_BRANCH            = 249;
    private static final int SP_BRANCH_CONDITIONAL = 250;
    private static final int SP_SELECTION_MERGE   = 247;
    private static final int SP_RETURN            = 253;

    private static final int SC_INPUT          = 1;
    private static final int SC_PUSH_CONSTANT  = 9;
    private static final int SC_STORAGE_BUFFER = 12;

    private static final int DECO_BLOCK       = 2;
    private static final int DECO_ARRAY_STRIDE = 6;
    private static final int DECO_BUILT_IN    = 11;
    private static final int DECO_BINDING     = 33;
    private static final int DECO_DESCRIPTOR_SET = 34;
    private static final int DECO_OFFSET      = 35;

    private static final int BUILTIN_GLOBAL_INVOCATION_ID = 28;

    private static final int CAP_SHADER  = 1;
    private static final int ADDR_LOGICAL = 0;
    private static final int MEM_GLSL450 = 1;
    private static final int EXEC_GL_COMPUTE = 5;
    private static final int EXEC_MODE_LOCAL_SIZE = 17;
    private static final int FUNC_NONE   = 0;

    private static final int GLSL_F_ABS = 4;
    private static final int GLSL_F_MIN = 37;
    private static final int GLSL_F_MAX = 40;

    private DensityShaderCompiler() {}

    static byte[] compile(float[] encodedProgram, int instructionCount) {
        return new Emitter(encodedProgram, instructionCount).emit();
    }

    private static final class Emitter {

        private final float[] prog;
        private final int instrCount;
        private final ArrayList<Integer> words = new ArrayList<>(512);
        private int nextId = 1;

        private final int tVoid, tBool, tFloat, tInt, tUint, tUvec3;
        private final int tRtaFloat, tBufStruct;
        private final int tPtrSbBuf, tPtrSbFloat;
        private final int tPtrInputUvec3;
        private final int tPcStruct, tPtrPc, tPtrPcInt;
        private final int tFunc;
        private final int idGlsl;
        private final int vGid, vCoords, vAux, vDensity, vPc;
        private final int cUint0, cUint1, cUint2, cUint3;
        private final int cInt0;
        private final int cFloat0, cFloat1;
        private final int fMain;

        private final Map<Integer, Integer> floatConstIds = new LinkedHashMap<>();
        private final Map<Integer, Integer> auxIndexConstIds = new LinkedHashMap<>();

        Emitter(float[] prog, int instrCount) {
            this.prog = prog;
            this.instrCount = instrCount;

            tVoid           = nextId++;
            tBool           = nextId++;
            tFloat          = nextId++;
            tInt            = nextId++;
            tUint           = nextId++;
            tUvec3          = nextId++;
            tRtaFloat       = nextId++;
            tBufStruct      = nextId++;
            tPtrSbBuf       = nextId++;
            tPtrSbFloat     = nextId++;
            tPtrInputUvec3  = nextId++;
            tPcStruct       = nextId++;
            tPtrPc          = nextId++;
            tPtrPcInt       = nextId++;
            tFunc           = nextId++;
            idGlsl          = nextId++;
            vGid            = nextId++;
            vCoords         = nextId++;
            vAux            = nextId++;
            vDensity        = nextId++;
            vPc             = nextId++;
            cUint0          = nextId++;
            cUint1          = nextId++;
            cUint2          = nextId++;
            cUint3          = nextId++;
            cInt0           = nextId++;
            cFloat0         = nextId++;
            cFloat1         = nextId++;
            fMain           = nextId++;

            for (int i = 0; i < instrCount; i++) {
                int off = i * 4;
                int op = (int) prog[off];
                if (op == HC_OP_CONST) {
                    int bits = Float.floatToRawIntBits(prog[off + 2]);
                    floatConstIds.computeIfAbsent(bits, k -> nextId++);
                } else if (op == HC_OP_AUX) {
                    int auxIndex = (int) prog[off + 2];
                    auxIndexConstIds.computeIfAbsent(auxIndex, k -> nextId++);
                }
            }
        }

        byte[] emit() {
            words.add(SPIRV_MAGIC);
            words.add(SPIRV_VERSION);
            words.add(SPIRV_GENERATOR);
            words.add(0);
            words.add(0);

            emitCapabilities();
            emitExtensions();
            emitMemoryModel();
            emitEntryPoint();
            emitExecutionMode();
            emitAnnotations();
            emitTypes();
            emitConstants();
            emitVariables();
            emitFunction();

            words.set(3, nextId);

            ByteBuffer buf = ByteBuffer.allocate(words.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
            for (int w : words) {
                buf.putInt(w);
            }
            return buf.array();
        }

        private void emitCapabilities() {
            op(SP_CAPABILITY, CAP_SHADER);
        }

        private void emitExtensions() {
            List<Integer> ops = new ArrayList<>();
            ops.add(idGlsl);
            ops.addAll(encodeString("GLSL.std.450"));
            opList(SP_EXT_INST_IMPORT, ops);
        }

        private void emitMemoryModel() {
            op(SP_MEMORY_MODEL, ADDR_LOGICAL, MEM_GLSL450);
        }

        private void emitEntryPoint() {
            List<Integer> ops = new ArrayList<>();
            ops.add(EXEC_GL_COMPUTE);
            ops.add(fMain);
            ops.addAll(encodeString("main"));
            ops.add(vGid);
            ops.add(vCoords);
            ops.add(vAux);
            ops.add(vDensity);
            ops.add(vPc);
            opList(SP_ENTRY_POINT, ops);
        }

        private void emitExecutionMode() {
            op(SP_EXECUTION_MODE, fMain, EXEC_MODE_LOCAL_SIZE, 256, 1, 1);
        }

        private void emitAnnotations() {
            op(SP_DECORATE, vGid, DECO_BUILT_IN, BUILTIN_GLOBAL_INVOCATION_ID);
            op(SP_DECORATE, tRtaFloat, DECO_ARRAY_STRIDE, 4);
            op(SP_DECORATE, tBufStruct, DECO_BLOCK);
            op(SP_MEMBER_DECORATE, tBufStruct, 0, DECO_OFFSET, 0);
            op(SP_DECORATE, vCoords, DECO_DESCRIPTOR_SET, 0);
            op(SP_DECORATE, vCoords, DECO_BINDING, 0);
            op(SP_DECORATE, vAux, DECO_DESCRIPTOR_SET, 0);
            op(SP_DECORATE, vAux, DECO_BINDING, 2);
            op(SP_DECORATE, vDensity, DECO_DESCRIPTOR_SET, 0);
            op(SP_DECORATE, vDensity, DECO_BINDING, 3);
            op(SP_DECORATE, tPcStruct, DECO_BLOCK);
            op(SP_MEMBER_DECORATE, tPcStruct, 0, DECO_OFFSET, 0);
            op(SP_MEMBER_DECORATE, tPcStruct, 1, DECO_OFFSET, 4);
        }

        private void emitTypes() {
            typeId(SP_TYPE_VOID, tVoid);
            typeId(SP_TYPE_BOOL, tBool);
            typeIdArgs(SP_TYPE_FLOAT, tFloat, 32);
            typeIdArgs(SP_TYPE_INT, tInt, 32, 1);
            typeIdArgs(SP_TYPE_INT, tUint, 32, 0);
            typeIdArgs(SP_TYPE_VECTOR, tUvec3, tUint, 3);
            typeIdArgs(SP_TYPE_RUNTIME_ARRAY, tRtaFloat, tFloat);
            typeIdArgs(SP_TYPE_STRUCT, tBufStruct, tRtaFloat);
            typeIdArgs(SP_TYPE_POINTER, tPtrSbBuf, SC_STORAGE_BUFFER, tBufStruct);
            typeIdArgs(SP_TYPE_POINTER, tPtrSbFloat, SC_STORAGE_BUFFER, tFloat);
            typeIdArgs(SP_TYPE_POINTER, tPtrInputUvec3, SC_INPUT, tUvec3);
            typeIdArgs(SP_TYPE_STRUCT, tPcStruct, tInt, tInt);
            typeIdArgs(SP_TYPE_POINTER, tPtrPc, SC_PUSH_CONSTANT, tPcStruct);
            typeIdArgs(SP_TYPE_POINTER, tPtrPcInt, SC_PUSH_CONSTANT, tInt);
            typeIdArgs(SP_TYPE_FUNCTION, tFunc, tVoid);
        }

        private void emitConstants() {
            constId(tUint, cUint0, 0);
            constId(tUint, cUint1, 1);
            constId(tUint, cUint2, 2);
            constId(tUint, cUint3, 3);
            constId(tInt, cInt0, 0);
            constId(tFloat, cFloat0, 0);
            constId(tFloat, cFloat1, Float.floatToRawIntBits(1.0f));

            for (Map.Entry<Integer, Integer> e : floatConstIds.entrySet()) {
                constId(tFloat, e.getValue(), e.getKey());
            }
            for (Map.Entry<Integer, Integer> e : auxIndexConstIds.entrySet()) {
                constId(tUint, e.getValue(), e.getKey());
            }
        }

        private void emitVariables() {
            varId(tPtrInputUvec3, vGid, SC_INPUT);
            varId(tPtrSbBuf, vCoords, SC_STORAGE_BUFFER);
            varId(tPtrSbBuf, vAux, SC_STORAGE_BUFFER);
            varId(tPtrSbBuf, vDensity, SC_STORAGE_BUFFER);
            varId(tPtrPc, vPc, SC_PUSH_CONSTANT);
        }

        private void emitFunction() {
            op(SP_FUNCTION, tVoid, fMain, FUNC_NONE, tFunc);
            int lblEntry = label();

            int gidVec      = res(SP_LOAD, tUvec3, vGid);
            int gidU        = res(SP_COMPOSITE_EXTRACT, tUint, gidVec, 0);
            int scPtr       = res(SP_ACCESS_CHAIN, tPtrPcInt, vPc, cInt0);
            int sampleCap   = res(SP_LOAD, tInt, scPtr);
            int gidI        = res(SP_BITCAST, tInt, gidU);
            int earlyOut    = res(SP_S_GREATER_THAN_EQUAL, tBool, gidI, sampleCap);

            int lblMerge    = nextId++;
            int lblExit     = nextId++;
            int lblBody     = nextId++;

            op(SP_SELECTION_MERGE, lblMerge, 0);
            op(SP_BRANCH_CONDITIONAL, earlyOut, lblExit, lblBody);

            op(SP_LABEL, lblExit);
            op(SP_RETURN);

            op(SP_LABEL, lblBody);

            int base3 = res(SP_I_MUL, tUint, gidU, cUint3);
            int xi    = base3;
            int yi    = res(SP_I_ADD, tUint, base3, cUint1);
            int zi    = res(SP_I_ADD, tUint, base3, cUint2);
            int xPtr  = res(SP_ACCESS_CHAIN, tPtrSbFloat, vCoords, cUint0, xi);
            int yPtr  = res(SP_ACCESS_CHAIN, tPtrSbFloat, vCoords, cUint0, yi);
            int zPtr  = res(SP_ACCESS_CHAIN, tPtrSbFloat, vCoords, cUint0, zi);

            int[] regIds = new int[64];
            regIds[0] = cFloat0;
            regIds[1] = res(SP_LOAD, tFloat, xPtr);
            regIds[2] = res(SP_LOAD, tFloat, yPtr);
            regIds[3] = res(SP_LOAD, tFloat, zPtr);

            int scU = res(SP_BITCAST, tUint, sampleCap);
            int[] auxLoaded = new int[auxIndexConstIds.isEmpty() ? 1 : auxIndexConstIds.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1];

            for (int i = 0; i < instrCount; i++) {
                int off    = i * 4;
                int opcode = (int) prog[off];
                int dst    = (int) prog[off + 1];
                int a      = (int) prog[off + 2];
                int b      = (int) prog[off + 3];
                float af   = prog[off + 2];
                float bf   = prog[off + 3];

                switch (opcode) {
                    case HC_OP_END -> {}
                    case HC_OP_CONST -> {
                        int bits = Float.floatToRawIntBits(af);
                        regIds[dst] = floatConstIds.get(bits);
                    }
                    case HC_OP_ADD -> regIds[dst] = res(SP_F_ADD, tFloat, regIds[a], regIds[b]);
                    case HC_OP_SUB -> regIds[dst] = res(SP_F_SUB, tFloat, regIds[a], regIds[b]);
                    case HC_OP_MUL -> regIds[dst] = res(SP_F_MUL, tFloat, regIds[a], regIds[b]);
                    case HC_OP_DIV -> regIds[dst] = res(SP_F_DIV, tFloat, regIds[a], regIds[b]);
                    case HC_OP_MIN -> regIds[dst] = extInst(GLSL_F_MIN, regIds[a], regIds[b]);
                    case HC_OP_MAX -> regIds[dst] = extInst(GLSL_F_MAX, regIds[a], regIds[b]);
                    case HC_OP_ABS -> regIds[dst] = extInst(GLSL_F_ABS, regIds[a]);
                    case HC_OP_SQUARE -> regIds[dst] = res(SP_F_MUL, tFloat, regIds[a], regIds[a]);
                    case HC_OP_CUBE -> {
                        int sq = res(SP_F_MUL, tFloat, regIds[a], regIds[a]);
                        regIds[dst] = res(SP_F_MUL, tFloat, sq, regIds[a]);
                    }
                    case HC_OP_NEG_MUL -> {
                        int mul = res(SP_F_MUL, tFloat, regIds[a], regIds[b]);
                        int gt  = res(SP_F_ORD_GREATER_THAN, tBool, regIds[a], cFloat0);
                        regIds[dst] = res(SP_SELECT, tFloat, gt, regIds[a], mul);
                    }
                    case HC_OP_GE -> {
                        int cmpGe = res(SP_F_ORD_GREATER_THAN_EQUAL, tBool, regIds[a], regIds[b]);
                        regIds[dst] = res(SP_SELECT, tFloat, cmpGe, cFloat1, cFloat0);
                    }
                    case HC_OP_LT -> {
                        int cmpLt = res(SP_F_ORD_LESS_THAN, tBool, regIds[a], regIds[b]);
                        regIds[dst] = res(SP_SELECT, tFloat, cmpLt, cFloat1, cFloat0);
                    }
                    case HC_OP_AUX -> {
                        int auxIndex = a;
                        if (auxLoaded[auxIndex] == 0) {
                            int auxConstId = auxIndexConstIds.get(auxIndex);
                            int auxBase    = res(SP_I_MUL, tUint, auxConstId, scU);
                            int auxOff     = res(SP_I_ADD, tUint, auxBase, gidU);
                            int auxPtr     = res(SP_ACCESS_CHAIN, tPtrSbFloat, vAux, cUint0, auxOff);
                            auxLoaded[auxIndex] = res(SP_LOAD, tFloat, auxPtr);
                        }
                        regIds[dst] = auxLoaded[auxIndex];
                    }
                    default -> {}
                }
                if (opcode == HC_OP_END) {
                    break;
                }
            }

            int outPtr = res(SP_ACCESS_CHAIN, tPtrSbFloat, vDensity, cUint0, gidU);
            op(SP_STORE, outPtr, regIds[0]);
            op(SP_BRANCH, lblMerge);

            op(SP_LABEL, lblMerge);
            op(SP_RETURN);
            op(SP_FUNCTION_END);
        }

        private int extInst(int glslOp, int... operands) {
            int id = nextId++;
            List<Integer> ops = new ArrayList<>();
            ops.add(tFloat);
            ops.add(id);
            ops.add(idGlsl);
            ops.add(glslOp);
            for (int o : operands) ops.add(o);
            opList(SP_EXT_INST, ops);
            return id;
        }

        private int res(int opcode, int typeId, int... operands) {
            int id = nextId++;
            int wc = 3 + operands.length;
            words.add((wc << 16) | opcode);
            words.add(typeId);
            words.add(id);
            for (int o : operands) words.add(o);
            return id;
        }

        private void op(int opcode, int... operands) {
            int wc = 1 + operands.length;
            words.add((wc << 16) | opcode);
            for (int o : operands) words.add(o);
        }

        private void opList(int opcode, List<Integer> operands) {
            int wc = 1 + operands.size();
            words.add((wc << 16) | opcode);
            for (int o : operands) words.add(o);
        }

        private int label() {
            int id = nextId++;
            op(SP_LABEL, id);
            return id;
        }

        private void typeId(int opcode, int id) {
            words.add((2 << 16) | opcode);
            words.add(id);
        }

        private void typeIdArgs(int opcode, int id, int... args) {
            int wc = 2 + args.length;
            words.add((wc << 16) | opcode);
            words.add(id);
            for (int a : args) words.add(a);
        }

        private void constId(int typeId, int id, int value) {
            words.add((4 << 16) | SP_CONSTANT);
            words.add(typeId);
            words.add(id);
            words.add(value);
        }

        private void varId(int typeId, int id, int storageClass) {
            words.add((4 << 16) | SP_VARIABLE);
            words.add(typeId);
            words.add(id);
            words.add(storageClass);
        }

        private static List<Integer> encodeString(String s) {
            byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            List<Integer> result = new ArrayList<>();
            int i = 0;
            while (i < bytes.length) {
                int w = 0;
                for (int j = 0; j < 4 && i + j < bytes.length; j++) {
                    w |= (bytes[i + j] & 0xFF) << (j * 8);
                }
                result.add(w);
                i += 4;
            }
            if (bytes.length % 4 == 0) {
                result.add(0);
            }
            return result;
        }
    }
}
