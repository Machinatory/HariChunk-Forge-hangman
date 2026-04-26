package com.hari.harichunk.opts.dfc.common.ast;

public enum EvalType {
    NORMAL, INTERPOLATION;

    public static EvalType fromContext(Object context) {
        return INTERPOLATION; // simplified: treat all as interpolation for safety
    }
}
