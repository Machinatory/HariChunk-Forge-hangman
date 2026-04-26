package com.hari.harichunk.opts.natives_math.common;

import com.hari.harichunk.opts.natives_math.common.isa.ISA_aarch64;
import com.hari.harichunk.opts.natives_math.common.isa.ISA_x86_64;

public interface ISATarget {

    int ordinal();

    String getSuffix();

    boolean isNativelySupported();

    static Class<? extends Enum<? extends ISATarget>> getInstance() {
        String arch = NativeLoader.NORMALIZED_ARCH;
        if ("x86_64".equals(arch)) {
            return ISA_x86_64.class;
        } else if ("aarch_64".equals(arch)) {
            return ISA_aarch64.class;
        }
        return null;
    }
}
