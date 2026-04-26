package com.hari.harichunk.opts.dfc.common.gen;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;
import com.hari.harichunk.opts.dfc.common.util.ArrayCache;

@FunctionalInterface
public interface IMultiMethod {

    void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type, ArrayCache arrayCache);

}
