package com.hari.harichunk.opts.dfc.common.gen;

import com.hari.harichunk.opts.dfc.common.ast.AstNode;
import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.util.ArrayCache;

import java.util.List;
import java.util.Objects;

final class GpuCompiledEntry implements CompiledEntry {

    private final CompiledEntry delegate;
    private final GpuDensityProgramCompiler.EncodedProgram program;

    private GpuCompiledEntry(CompiledEntry delegate, GpuDensityProgramCompiler.EncodedProgram program) {
        this.delegate = Objects.requireNonNull(delegate);
        this.program = Objects.requireNonNull(program);
    }

    static CompiledEntry wrap(CompiledEntry delegate, GpuDensityProgramCompiler.EncodedProgram program) {
        return program != null ? new GpuCompiledEntry(delegate, program) : delegate;
    }

    @Override
    public double evalSingle(int x, int y, int z, EvalType type) {
        return delegate.evalSingle(x, y, z, type);
    }

    @Override
    public void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type, ArrayCache arrayCache) {
        float[] auxValues = program.hasAuxNodes()
            ? computeAuxValues(program.auxNodes(), res.length, x, y, z, type, arrayCache)
            : null;
        if (GpuDensityFunction.tryEvaluateEncodedProgram(program.encoded(), program.instructionCount(), res, x, y, z,
            auxValues, program.auxNodes().size())) {
            return;
        }
        delegate.evalMulti(res, x, y, z, type, arrayCache);
    }

    private static float[] computeAuxValues(List<AstNode> auxNodes,
                                            int sampleCount,
                                            int[] x,
                                            int[] y,
                                            int[] z,
                                            EvalType type,
                                            ArrayCache arrayCache) {
        float[] auxValues = new float[auxNodes.size() * sampleCount];
        boolean[] nativeFilled = GpuNativeNoiseLeafEvaluator.tryEvaluateAll(auxNodes, sampleCount, x, y, z, auxValues);
        for (int auxIndex = 0; auxIndex < auxNodes.size(); auxIndex++) {
            if (nativeFilled[auxIndex]) {
                continue;
            }

            int outputOffset = auxIndex * sampleCount;
            double[] values = arrayCache != null
                ? arrayCache.getDoubleArray(sampleCount, false)
                : new double[sampleCount];
            try {
                auxNodes.get(auxIndex).evalMulti(values, x, y, z, type);
                for (int i = 0; i < sampleCount; i++) {
                    auxValues[outputOffset + i] = (float) values[i];
                }
            } finally {
                if (arrayCache != null) {
                    arrayCache.recycle(values);
                }
            }
        }
        return auxValues;
    }

    @Override
    public CompiledEntry newInstance(List<?> args) {
        return wrap(delegate.newInstance(args), program);
    }

    @Override
    public List<Object> getArgs() {
        return delegate.getArgs();
    }
}
