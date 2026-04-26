package com.hari.harichunk.opts.dfc.common.gen;

import com.hari.harichunk.opts.dfc.common.ast.EvalType;

@FunctionalInterface
public interface ISingleMethod {

    double evalSingle(int x, int y, int z, EvalType type);

}
