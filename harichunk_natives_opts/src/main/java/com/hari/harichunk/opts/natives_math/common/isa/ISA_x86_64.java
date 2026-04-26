package com.hari.harichunk.opts.natives_math.common.isa;

import com.hari.harichunk.opts.natives_math.common.ISATarget;

public enum ISA_x86_64 implements ISATarget {
    SSE2("_sse2", true),
    SSE4_1("_sse2", false),
    SSE4_2("_sse4_2", true),
    AVX("_avx", true),
    AVX2("_avx2", true),
    AVX2ADL("_avx2adl", true),
    AVX512KNL("_avx2", false),
    AVX512SKX("_avx512skx", true),
    AVX512ICL("_avx512icl", true),
    AVX512SPR("_avx512spr", true);

    private final String suffix;
    private final boolean nativelySupported;

    ISA_x86_64(String suffix, boolean nativelySupported) {
        this.suffix = suffix;
        this.nativelySupported = nativelySupported;
    }

    @Override
    public String getSuffix() { return this.suffix; }

    @Override
    public boolean isNativelySupported() { return this.nativelySupported; }
}
