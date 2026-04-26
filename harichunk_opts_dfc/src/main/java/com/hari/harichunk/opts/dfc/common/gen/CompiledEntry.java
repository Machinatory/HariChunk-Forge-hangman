package com.hari.harichunk.opts.dfc.common.gen;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.util.ArrayCache;

import java.util.List;

public interface CompiledEntry extends ISingleMethod, IMultiMethod {

    double evalSingle(int x, int y, int z, EvalType type);

    void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type, ArrayCache arrayCache);

    CompiledEntry newInstance(List<?> args);

    List<Object> getArgs();

}
