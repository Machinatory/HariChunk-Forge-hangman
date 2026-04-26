package com.hari.harichunk.opts.natives_math.common;

public class NativeBindings {

    private NativeBindings() {}

    // --- JNI native methods ---
    public static native int getSystemISA(boolean allowAVX512);
    public static native double noisePerlinDouble(long dataPtr, double x, double y, double z);
    public static native void noisePerlinDoubleBatch(long dataPtr, long resPtr, long xPtr, long yPtr, long zPtr, int length);
    public static native double noiseInterpolated(long dataPtr, double x, double y, double z);
    public static native float endIslandsSample(long permPtr, int x, int z);
    public static native int biomeAccessSample(long seed, int x, int y, int z);

    // --- Batch helper using Unsafe for array pinning ---
    public static void noisePerlinDoubleBatch(long dataPtr, double[] res, double[] x, double[] y, double[] z, int length) {
        long resPtr = NativeStructs.pinArray(res);
        long xPtr = NativeStructs.pinArray(x);
        long yPtr = NativeStructs.pinArray(y);
        long zPtr = NativeStructs.pinArray(z);
        try {
            noisePerlinDoubleBatch(dataPtr, resPtr, xPtr, yPtr, zPtr, length);
        } finally {
            NativeStructs.releaseArray(res, resPtr);
            NativeStructs.releaseArray(x, xPtr);
            NativeStructs.releaseArray(y, yPtr);
            NativeStructs.releaseArray(z, zPtr);
        }
    }
}
