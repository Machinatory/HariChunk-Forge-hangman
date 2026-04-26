package com.hari.harichunk.opts.dfc.common.gen;

import com.hari.harichunk.opts.dfc.common.ast.AstNode;
import com.hari.harichunk.opts.dfc.common.ast.noise.DFTNoiseNode;
import com.hari.harichunk.opts.dfc.common.ast.noise.DFTShiftANode;
import com.hari.harichunk.opts.dfc.common.ast.noise.DFTShiftBNode;
import com.hari.harichunk.opts.dfc.common.ast.noise.DFTShiftNode;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

final class GpuNativeNoiseLeafEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk/DFC-BRS-GPU-Noise");

    private static final String BRS_GPU_NOISE_CLASS = "org.admany.brsgpunoise.BrsGpuNoise";
    private static volatile Method nativeNoiseBatchMethod;
    private static volatile boolean unavailable;

    private GpuNativeNoiseLeafEvaluator() {
    }

    static boolean[] tryEvaluateAll(List<AstNode> nodes,
                                    int sampleCount,
                                    int[] x,
                                    int[] y,
                                    int[] z,
                                    float[] auxValues) {
        boolean[] filled = new boolean[nodes.size()];
        if (sampleCount <= 0 || unavailable || nodes.isEmpty()) {
            return filled;
        }

        NativeNoiseRequest[] requests = new NativeNoiseRequest[nodes.size()];
        int nativeCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            NativeNoiseRequest request = requestFor(nodes.get(i));
            if (request != null) {
                requests[i] = request;
                nativeCount++;
            }
        }
        if (nativeCount == 0) {
            return filled;
        }

        int[] modes = new int[nativeCount];
        double[] xzScales = new double[nativeCount];
        double[] yScales = new double[nativeCount];
        int[] noiseKeys = new int[nativeCount];
        float[] outputScales = new float[nativeCount];
        int[] originalIndexes = new int[nativeCount];

        int nativeIndex = 0;
        for (int i = 0; i < requests.length; i++) {
            NativeNoiseRequest request = requests[i];
            if (request == null) {
                continue;
            }
            modes[nativeIndex] = request.mode;
            xzScales[nativeIndex] = request.xzScale;
            yScales[nativeIndex] = request.yScale;
            noiseKeys[nativeIndex] = stableNoiseKey(request.noiseHolder);
            outputScales[nativeIndex] = request.outputScale;
            originalIndexes[nativeIndex] = i;
            nativeIndex++;
        }

        try {
            Method method = nativeNoiseBatchMethod;
            if (method == null) {
                method = Class.forName(BRS_GPU_NOISE_CLASS).getMethod(
                        "tryComputeNativeNoiseLeafBatches",
                        int[].class,
                        int[].class,
                        int[].class,
                        int[].class,
                        int.class,
                        int.class,
                        double[].class,
                        double[].class,
                        int[].class,
                        float[].class
                );
                nativeNoiseBatchMethod = method;
            }

            Object result = method.invoke(null,
                    modes,
                    x,
                    y,
                    z,
                    0,
                    sampleCount,
                    xzScales,
                    yScales,
                    noiseKeys,
                    outputScales);
            if (!(result instanceof double[] values) || values.length != nativeCount * sampleCount) {
                return filled;
            }

            for (int i = 0; i < nativeCount; i++) {
                int auxIndex = originalIndexes[i];
                int srcOffset = i * sampleCount;
                int dstOffset = auxIndex * sampleCount;
                for (int sample = 0; sample < sampleCount; sample++) {
                    auxValues[dstOffset + sample] = (float) values[srcOffset + sample];
                }
                filled[auxIndex] = true;
            }
            return filled;
        } catch (ClassNotFoundException ignored) {
            unavailable = true;
            return filled;
        } catch (Throwable throwable) {
            LOGGER.debug("BRS native GPU noise multi-leaf unavailable: {}", throwable.toString());
            return filled;
        }
    }

    private static NativeNoiseRequest requestFor(AstNode node) {
        if (node instanceof DFTNoiseNode noise) {
            return new NativeNoiseRequest(0, noise.noiseHolder(), noise.xzScale(), noise.yScale(), 1.0f);
        }
        if (node instanceof DFTShiftNode shift) {
            return new NativeNoiseRequest(1, shift.offsetNoise(), 0.25d, 0.25d, 4.0f);
        }
        if (node instanceof DFTShiftANode shift) {
            return new NativeNoiseRequest(2, shift.offsetNoise(), 0.25d, 0.25d, 4.0f);
        }
        if (node instanceof DFTShiftBNode shift) {
            return new NativeNoiseRequest(3, shift.offsetNoise(), 0.25d, 0.25d, 4.0f);
        }
        return null;
    }

    private static int stableNoiseKey(DensityFunction.NoiseHolder noiseHolder) {
        try {
            Method method = noiseHolder.getClass().getMethod("noiseData");
            Object noiseData = method.invoke(noiseHolder);
            return String.valueOf(noiseData).hashCode();
        } catch (Throwable ignored) {
            return String.valueOf(noiseHolder).hashCode();
        }
    }

    private record NativeNoiseRequest(int mode,
                                      DensityFunction.NoiseHolder noiseHolder,
                                      double xzScale,
                                      double yScale,
                                      float outputScale) {
    }
}
