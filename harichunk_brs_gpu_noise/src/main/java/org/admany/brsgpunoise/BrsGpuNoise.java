/*
 * HariChunk BlackRift Studios Vulkan GPU Noise by Admany / BlackRift Studios.
 * Licensed strictly under BRSSLA V1.5.
 * This is original BlackRift Studios code and is not related to C2ME in any way.
 *
 * This source may be viewed and contributed to through approved project channels.
 * Modification, copying, redistribution, reuse, or derivative use outside those
 * approved contribution flows is strictly not allowed. C2ME may not copy, modify,
 * reuse, redistribute, or derive from this code.
 */
package org.admany.brsgpunoise;

import org.admany.vkgpuaccel.AdaptiveBatchGate;
import org.admany.vkgpuaccel.VkGpuAccel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BrsGpuNoise {

    public static final int MODE_DFT_NOISE = 0;
    public static final int MODE_SHIFT = 1;
    public static final int MODE_SHIFT_A = 2;
    public static final int MODE_SHIFT_B = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk BRS GPU Noise");
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AdaptiveBatchGate NATIVE_NOISE_GATE =
            new AdaptiveBatchGate(BrsGpuNoiseConfig.MAX_ADAPTIVE_BATCH_THRESHOLD);

    private static volatile boolean ready;
    private static volatile String unavailableReason = "not initialized";

    private BrsGpuNoise() {
    }

    public static void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        refreshAvailability();
    }

    public static boolean refreshAvailability() {
        if (!BrsGpuNoiseConfig.ENABLED) {
            ready = false;
            unavailableReason = "disabled";
            return false;
        }
        if (!BrsGpuNoiseConfig.NATIVE_NOISE) {
            ready = false;
            unavailableReason = "native noise disabled";
            return false;
        }

        try {
            ready = VkGpuAccel.shouldOwnGpuNoise();
            unavailableReason = ready ? "" : "QAPI Vulkan owner unavailable";
            return ready;
        } catch (Throwable throwable) {
            ready = false;
            unavailableReason = throwable.toString();
            LOGGER.debug("BRS GPU noise availability check failed: {}", throwable.toString());
            return false;
        }
    }

    public static boolean isReady() {
        return ready || refreshAvailability();
    }

    public static String statusString() {
        return isReady() ? "ready, gate=" + NATIVE_NOISE_GATE.summary() + " " + BrsGpuNoiseStats.summary()
                : "unavailable: " + unavailableReason;
    }

    public static String debugString() {
        return "ready=" + isReady()
                + " gate=" + NATIVE_NOISE_GATE.summary()
                + " nativeNoise=" + BrsGpuNoiseConfig.NATIVE_NOISE
                + " stats=" + BrsGpuNoiseStats.summary();
    }

    public static double[] tryComputeNativeNoiseLeafBatch(int mode,
                                                          int[] xCoords,
                                                          int[] yCoords,
                                                          int[] zCoords,
                                                          int offset,
                                                          int length,
                                                          double xzScale,
                                                          double yScale,
                                                          int noiseKey,
                                                          float outputScale) {
        Objects.requireNonNull(xCoords, "xCoords");
        Objects.requireNonNull(yCoords, "yCoords");
        Objects.requireNonNull(zCoords, "zCoords");
        if (!isReady()) {
            BrsGpuNoiseStats.recordUnavailable();
            return null;
        }
        if (!NATIVE_NOISE_GATE.shouldAttempt(length)) {
            BrsGpuNoiseStats.recordAdaptiveSkip();
            return null;
        }
        if (offset < 0 || length < 0 || offset + length > xCoords.length
                || offset + length > yCoords.length || offset + length > zCoords.length) {
            BrsGpuNoiseStats.recordInvalidInput();
            return null;
        }

        BrsGpuNoiseStats.recordAttempt();

        float[] packed = new float[length * 3];
        packCoordinates(mode, xCoords, yCoords, zCoords, offset, length, xzScale, yScale, noiseKey, packed, 0);

        try {
            float[] features = VkGpuAccel.submitTerrainFeatureBatch(packed)
                    .get(Math.max(1L, BrsGpuNoiseConfig.TASK_TIMEOUT.toMillis()), TimeUnit.MILLISECONDS);
            if (features == null || features.length < length * 4) {
                NATIVE_NOISE_GATE.recordFailure();
                BrsGpuNoiseStats.recordNullResult();
                return null;
            }

            double[] result = new double[length];
            double scale = outputScale;
            for (int i = 0; i < length; i++) {
                int base = i * 4;
                double primary = features[base] * 2.0d - 1.0d;
                double detail = features[base + 1] * 0.0625d + features[base + 2] * 0.015625d;
                result[i] = clamp(primary + detail, -1.0d, 1.0d) * scale;
            }
            BrsGpuNoiseStats.recordNativeNoiseBatch(length);
            NATIVE_NOISE_GATE.recordSuccess();
            BrsGpuNoiseStats.recordSuccess();
            return result;
        } catch (Throwable throwable) {
            NATIVE_NOISE_GATE.recordFailure();
            BrsGpuNoiseStats.recordFailure();
            LOGGER.debug("BRS native GPU noise batch failed: {}", throwable.toString());
            return null;
        }
    }

    public static double[] tryComputeNativeNoiseLeafBatches(int[] modes,
                                                            int[] xCoords,
                                                            int[] yCoords,
                                                            int[] zCoords,
                                                            int offset,
                                                            int length,
                                                            double[] xzScales,
                                                            double[] yScales,
                                                            int[] noiseKeys,
                                                            float[] outputScales) {
        Objects.requireNonNull(modes, "modes");
        Objects.requireNonNull(xCoords, "xCoords");
        Objects.requireNonNull(yCoords, "yCoords");
        Objects.requireNonNull(zCoords, "zCoords");
        Objects.requireNonNull(xzScales, "xzScales");
        Objects.requireNonNull(yScales, "yScales");
        Objects.requireNonNull(noiseKeys, "noiseKeys");
        Objects.requireNonNull(outputScales, "outputScales");

        int leafCount = modes.length;
        if (leafCount == 0) {
            return new double[0];
        }
        if (xzScales.length != leafCount || yScales.length != leafCount
                || noiseKeys.length != leafCount || outputScales.length != leafCount) {
            BrsGpuNoiseStats.recordInvalidInput();
            return null;
        }
        if (!isReady()) {
            BrsGpuNoiseStats.recordUnavailable();
            return null;
        }
        if (!NATIVE_NOISE_GATE.shouldAttempt(length)) {
            BrsGpuNoiseStats.recordAdaptiveSkip();
            return null;
        }
        if (offset < 0 || length < 0 || offset + length > xCoords.length
                || offset + length > yCoords.length || offset + length > zCoords.length) {
            BrsGpuNoiseStats.recordInvalidInput();
            return null;
        }

        BrsGpuNoiseStats.recordAttempt();

        int packedSamples = leafCount * length;
        float[] packed = new float[packedSamples * 3];
        for (int leaf = 0; leaf < leafCount; leaf++) {
            packCoordinates(modes[leaf], xCoords, yCoords, zCoords, offset, length,
                    xzScales[leaf], yScales[leaf], noiseKeys[leaf], packed, leaf * length * 3);
        }

        try {
            float[] features = VkGpuAccel.submitTerrainFeatureBatch(packed)
                    .get(Math.max(1L, BrsGpuNoiseConfig.TASK_TIMEOUT.toMillis()), TimeUnit.MILLISECONDS);
            if (features == null || features.length < packedSamples * 4) {
                NATIVE_NOISE_GATE.recordFailure();
                BrsGpuNoiseStats.recordNullResult();
                return null;
            }

            double[] result = new double[packedSamples];
            for (int leaf = 0; leaf < leafCount; leaf++) {
                double scale = outputScales[leaf];
                int resultBase = leaf * length;
                int featureBase = resultBase * 4;
                for (int i = 0; i < length; i++) {
                    int base = featureBase + i * 4;
                    double primary = features[base] * 2.0d - 1.0d;
                    double detail = features[base + 1] * 0.0625d + features[base + 2] * 0.015625d;
                    result[resultBase + i] = clamp(primary + detail, -1.0d, 1.0d) * scale;
                }
            }

            BrsGpuNoiseStats.recordNativeNoiseBatch(packedSamples);
            NATIVE_NOISE_GATE.recordSuccess();
            BrsGpuNoiseStats.recordSuccess();
            return result;
        } catch (Throwable throwable) {
            NATIVE_NOISE_GATE.recordFailure();
            BrsGpuNoiseStats.recordFailure();
            LOGGER.debug("BRS native GPU noise multi-leaf batch failed: {}", throwable.toString());
            return null;
        }
    }

    private static void packCoordinates(int mode,
                                        int[] xCoords,
                                        int[] yCoords,
                                        int[] zCoords,
                                        int offset,
                                        int length,
                                        double xzScale,
                                        double yScale,
                                        int noiseKey,
                                        float[] packed,
                                        int targetOffset) {
        double seedX = ((noiseKey) & 0xffff) * (1.0d / 2048.0d);
        double seedY = ((noiseKey >>> 8) & 0xffff) * (1.0d / 2048.0d);
        double seedZ = ((noiseKey >>> 16) & 0xffff) * (1.0d / 2048.0d);

        for (int i = 0; i < length; i++) {
            int source = offset + i;
            double x;
            double y;
            double z;
            switch (mode) {
                case MODE_DFT_NOISE -> {
                    x = xCoords[source] * xzScale;
                    y = yCoords[source] * yScale;
                    z = zCoords[source] * xzScale;
                }
                case MODE_SHIFT -> {
                    x = xCoords[source] * 0.25d;
                    y = yCoords[source] * 0.25d;
                    z = zCoords[source] * 0.25d;
                }
                case MODE_SHIFT_A -> {
                    x = xCoords[source] * 0.25d;
                    y = 0.0d;
                    z = zCoords[source] * 0.25d;
                }
                case MODE_SHIFT_B -> {
                    x = zCoords[source] * 0.25d;
                    y = xCoords[source] * 0.25d;
                    z = 0.0d;
                }
                default -> throw new IllegalArgumentException("Unknown BRS GPU noise mode: " + mode);
            }

            int target = targetOffset + i * 3;
            packed[target] = (float) (x + seedX);
            packed[target + 1] = (float) (y + seedY);
            packed[target + 2] = (float) (z + seedZ);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
