package com.hari.harichunk.opts.natives_math.common;

import com.hari.harichunk.base.mixin.access.IBlendedNoise;
import com.hari.harichunk.base.mixin.access.IOctavePerlinNoiseSampler;
import com.hari.harichunk.base.mixin.access.IPerlinNoiseSampler;
import com.hari.harichunk.opts.natives_math.common.util.MemoryUtil;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.stream.IntStream;

public class NativeStructs {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Unsafe instance", e);
        }
    }

    // double_octave_sampler_data_t offsets (total 80 bytes, alignment 32)
    public static final long DOUBLE_OCTAVE_SIZE = 80L;
    public static final long OFF_LENGTH = 0;
    public static final long OFF_AMPLITUDE = 8;
    public static final long OFF_NEED_SHIFT = 16;
    public static final long OFF_LACUNARITY_POWD = 24;
    public static final long OFF_PERSISTENCE_POWD = 32;
    public static final long OFF_SAMPLER_PERMUTATIONS = 40;
    public static final long OFF_SAMPLER_ORIGIN_X = 48;
    public static final long OFF_SAMPLER_ORIGIN_Y = 56;
    public static final long OFF_SAMPLER_ORIGIN_Z = 64;
    public static final long OFF_AMPLITUDES = 72;

    // interpolated_noise_sub_sampler_t offsets (total 48 bytes)
    public static final long SUB_SAMPLER_SIZE = 48L;
    public static final long SUB_OFF_PERMUTATIONS = 0;
    public static final long SUB_OFF_ORIGIN_X = 8;
    public static final long SUB_OFF_ORIGIN_Y = 16;
    public static final long SUB_OFF_ORIGIN_Z = 24;
    public static final long SUB_OFF_MUL_FACTOR = 32;
    public static final long SUB_OFF_LENGTH = 40;

    // interpolated_noise_sampler_t offsets (total 200 bytes, alignment 32)
    public static final long INTERPOLATED_SIZE = 200L;
    public static final long OFF_SCALED_XZ_SCALE = 0;
    public static final long OFF_SCALED_Y_SCALE = 8;
    public static final long OFF_XZ_FACTOR = 16;
    public static final long OFF_Y_FACTOR = 24;
    public static final long OFF_SMEAR_SCALE_MULTIPLIER = 32;
    public static final long OFF_XZ_SCALE_INTERP = 40;
    public static final long OFF_Y_SCALE_INTERP = 48;
    public static final long OFF_NORMAL_SUB = 56;
    public static final long OFF_UPPER_SUB = 104;
    public static final long OFF_LOWER_SUB = 152;

    public static long allocateMemory(long size) {
        long addr = UNSAFE.allocateMemory(size);
        UNSAFE.setMemory(addr, size, (byte) 0);
        return addr;
    }

    public static void freeMemory(long address) {
        UNSAFE.freeMemory(address);
    }

    public static void putLong(long addr, long value) { UNSAFE.putLong(addr, value); }
    public static void putDouble(long addr, double value) { UNSAFE.putDouble(addr, value); }
    public static void putInt(long addr, int value) { UNSAFE.putInt(addr, value); }
    public static void putByte(long addr, byte value) { UNSAFE.putByte(addr, value); }

    public static long getAddress(long structAddr, long offset) { return UNSAFE.getLong(structAddr + offset); }
    public static void putAddress(long structAddr, long offset, long value) { UNSAFE.putLong(structAddr + offset, value); }

    public static void copyIntArray(long dest, int[] src) {
        for (int i = 0; i < src.length; i++) {
            UNSAFE.putInt(dest + i * 4L, src[i]);
        }
    }

    public static long allocateAligned(long size, long alignment) {
        long raw = UNSAFE.allocateMemory(size + alignment);
        long aligned = (raw + alignment - 1) & ~(alignment - 1);
        // store original pointer before aligned address for freeing
        UNSAFE.putLong(aligned - 8, raw);
        return aligned;
    }

    public static void freeAligned(long aligned) {
        long raw = UNSAFE.getLong(aligned - 8);
        UNSAFE.freeMemory(raw);
    }

    public static long pinArray(double[] arr) {
        // Use Unsafe to get native array base offset and copy
        long baseOffset = UNSAFE.arrayBaseOffset(double[].class);
        long scale = UNSAFE.arrayIndexScale(double[].class);
        long size = arr.length * scale;
        long ptr = UNSAFE.allocateMemory(size);
        UNSAFE.copyMemory(arr, baseOffset, null, ptr, size);
        return ptr;
    }

    public static void releaseArray(double[] arr, long ptr) {
        if (ptr != 0 && arr != null) {
            long scale = UNSAFE.arrayIndexScale(double[].class);
            long size = arr.length * scale;
            UNSAFE.copyMemory(null, ptr, arr, UNSAFE.arrayBaseOffset(double[].class), size);
            UNSAFE.freeMemory(ptr);
        }
    }

    // --- Struct creation methods ---

    public static long createDoubleOctaveSamplerData(PerlinNoise firstSampler, PerlinNoise secondSampler, double amplitude) {
        long nonNullSamplerCount = 0;
        for (ImprovedNoise sampler : ((IOctavePerlinNoiseSampler) firstSampler).getNoiseLevels()) {
            if (sampler != null) nonNullSamplerCount++;
        }
        for (ImprovedNoise sampler : ((IOctavePerlinNoiseSampler) secondSampler).getNoiseLevels()) {
            if (sampler != null) nonNullSamplerCount++;
        }

        long data = allocateAligned(DOUBLE_OCTAVE_SIZE, 64);
        long needShift = allocateAligned(nonNullSamplerCount, 64);
        long lacunarityPowd = allocateAligned(nonNullSamplerCount * 8, 64);
        long persistencePowd = allocateAligned(nonNullSamplerCount * 8, 64);
        long samplerPerms = allocateAligned(nonNullSamplerCount * 256 * 4, 64);
        long samplerOriginX = allocateAligned(nonNullSamplerCount * 8, 64);
        long samplerOriginY = allocateAligned(nonNullSamplerCount * 8, 64);
        long samplerOriginZ = allocateAligned(nonNullSamplerCount * 8, 64);
        long amplitudes = allocateAligned(nonNullSamplerCount * 8, 64);

        putLong(data + OFF_LENGTH, nonNullSamplerCount);
        putDouble(data + OFF_AMPLITUDE, amplitude);
        putAddress(data, OFF_NEED_SHIFT, needShift);
        putAddress(data, OFF_LACUNARITY_POWD, lacunarityPowd);
        putAddress(data, OFF_PERSISTENCE_POWD, persistencePowd);
        putAddress(data, OFF_SAMPLER_PERMUTATIONS, samplerPerms);
        putAddress(data, OFF_SAMPLER_ORIGIN_X, samplerOriginX);
        putAddress(data, OFF_SAMPLER_ORIGIN_Y, samplerOriginY);
        putAddress(data, OFF_SAMPLER_ORIGIN_Z, samplerOriginZ);
        putAddress(data, OFF_AMPLITUDES, amplitudes);

        IOctavePerlinNoiseSampler firstAccess = (IOctavePerlinNoiseSampler) firstSampler;
        IOctavePerlinNoiseSampler secondAccess = (IOctavePerlinNoiseSampler) secondSampler;
        double firstLacunarity = firstAccess.getLowestFreqInputFactor();
        double firstPersistence = firstAccess.getLowestFreqValueFactor();
        double secondLacunarity = secondAccess.getLowestFreqInputFactor();
        double secondPersistence = secondAccess.getLowestFreqValueFactor();

        long index = 0;
        ImprovedNoise[] firstLevels = firstAccess.getNoiseLevels();
        for (int i = 0; i < firstLevels.length; i++) {
            ImprovedNoise sampler = firstLevels[i];
            if (sampler != null) {
                putByte(needShift + index, (byte) 0);
                putDouble(lacunarityPowd + index * 8, firstLacunarity * Math.pow(2.0, i));
                putDouble(persistencePowd + index * 8, firstPersistence * Math.pow(2.0, -i));
                copyIntArray(samplerPerms + index * 256 * 4, MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getP()));
                putDouble(samplerOriginX + index * 8, sampler.xo);
                putDouble(samplerOriginY + index * 8, sampler.yo);
                putDouble(samplerOriginZ + index * 8, sampler.zo);
                putDouble(amplitudes + index * 8, firstAccess.getAmplitudes().getDouble(i));
                index++;
            }
        }
        ImprovedNoise[] secondLevels = secondAccess.getNoiseLevels();
        for (int i = 0; i < secondLevels.length; i++) {
            ImprovedNoise sampler = secondLevels[i];
            if (sampler != null) {
                putByte(needShift + index, (byte) 1);
                putDouble(lacunarityPowd + index * 8, secondLacunarity * Math.pow(2.0, i));
                putDouble(persistencePowd + index * 8, secondPersistence * Math.pow(2.0, -i));
                copyIntArray(samplerPerms + index * 256 * 4, MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getP()));
                putDouble(samplerOriginX + index * 8, sampler.xo);
                putDouble(samplerOriginY + index * 8, sampler.yo);
                putDouble(samplerOriginZ + index * 8, sampler.zo);
                putDouble(amplitudes + index * 8, secondAccess.getAmplitudes().getDouble(i));
                index++;
            }
        }

        UNSAFE.fullFence();
        return data;
    }

    public static boolean isSpecializedBase3dNoiseFunction(BlendedNoise blended) {
        IBlendedNoise access = (IBlendedNoise) blended;
        return IntStream.range(0, 16).mapToObj(access.getMinLimitNoise()::getOctaveNoise).filter(Objects::nonNull).count() == 16 &&
                IntStream.range(0, 16).mapToObj(access.getMaxLimitNoise()::getOctaveNoise).filter(Objects::nonNull).count() == 16 &&
                IntStream.range(0, 8).mapToObj(access.getMainNoise()::getOctaveNoise).filter(Objects::nonNull).count() == 8;
    }

    public static long createInterpolatedNoiseSampler(BlendedNoise blended) {
        IBlendedNoise access = (IBlendedNoise) blended;
        long data = allocateAligned(INTERPOLATED_SIZE, 64);

        putDouble(data + OFF_SCALED_XZ_SCALE, access.getXzMultiplier());
        putDouble(data + OFF_SCALED_Y_SCALE, access.getYMultiplier());
        putDouble(data + OFF_XZ_FACTOR, access.getXzFactor());
        putDouble(data + OFF_Y_FACTOR, access.getYFactor());
        putDouble(data + OFF_SMEAR_SCALE_MULTIPLIER, access.getSmearScaleMultiplier());
        putDouble(data + OFF_XZ_SCALE_INTERP, access.getXzScale());
        putDouble(data + OFF_Y_SCALE_INTERP, access.getYScale());

        long samplerPerms = allocateAligned(40 * 256L * 4, 64);
        long samplerOriginX = allocateAligned(40 * 8L, 64);
        long samplerOriginY = allocateAligned(40 * 8L, 64);
        long samplerOriginZ = allocateAligned(40 * 8L, 64);
        long samplerMulFactor = allocateAligned(40 * 8L, 64);

        int index = 0;
        // Normal (main/interpolation) sub-sampler
        {
            int start = index;
            for (int i = 0; i < 8; i++) {
                ImprovedNoise sampler = access.getMainNoise().getOctaveNoise(i);
                if (sampler != null) {
                    copyIntArray(samplerPerms + index * 256L * 4, MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getP()));
                    putDouble(samplerOriginX + index * 8L, sampler.xo);
                    putDouble(samplerOriginY + index * 8L, sampler.yo);
                    putDouble(samplerOriginZ + index * 8L, sampler.zo);
                    putDouble(samplerMulFactor + index * 8L, Math.pow(2, -i));
                    index++;
                }
            }
            writeSubSampler(data, OFF_NORMAL_SUB, samplerPerms + start * 256L * 4, samplerOriginX + start * 8L,
                    samplerOriginY + start * 8L, samplerOriginZ + start * 8L, samplerMulFactor + start * 8L, index - start);
        }
        // Lower (minLimit) sub-sampler
        {
            index = 8;
            int start = index;
            for (int i = 0; i < 16; i++) {
                ImprovedNoise sampler = access.getMinLimitNoise().getOctaveNoise(i);
                if (sampler != null) {
                    copyIntArray(samplerPerms + index * 256L * 4, MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getP()));
                    putDouble(samplerOriginX + index * 8L, sampler.xo);
                    putDouble(samplerOriginY + index * 8L, sampler.yo);
                    putDouble(samplerOriginZ + index * 8L, sampler.zo);
                    putDouble(samplerMulFactor + index * 8L, Math.pow(2, -i));
                    index++;
                }
            }
            writeSubSampler(data, OFF_LOWER_SUB, samplerPerms + start * 256L * 4, samplerOriginX + start * 8L,
                    samplerOriginY + start * 8L, samplerOriginZ + start * 8L, samplerMulFactor + start * 8L, index - start);
        }
        // Upper (maxLimit) sub-sampler
        {
            index = 8 + 16;
            int start = index;
            for (int i = 0; i < 16; i++) {
                ImprovedNoise sampler = access.getMaxLimitNoise().getOctaveNoise(i);
                if (sampler != null) {
                    copyIntArray(samplerPerms + index * 256L * 4, MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getP()));
                    putDouble(samplerOriginX + index * 8L, sampler.xo);
                    putDouble(samplerOriginY + index * 8L, sampler.yo);
                    putDouble(samplerOriginZ + index * 8L, sampler.zo);
                    putDouble(samplerMulFactor + index * 8L, Math.pow(2, -i));
                    index++;
                }
            }
            writeSubSampler(data, OFF_UPPER_SUB, samplerPerms + start * 256L * 4, samplerOriginX + start * 8L,
                    samplerOriginY + start * 8L, samplerOriginZ + start * 8L, samplerMulFactor + start * 8L, index - start);
        }

        UNSAFE.fullFence();
        return data;
    }

    private static void writeSubSampler(long data, long subOffset, long perms, long originX, long originY, long originZ, long mulFactor, int length) {
        putAddress(data, subOffset + SUB_OFF_PERMUTATIONS, perms);
        putAddress(data, subOffset + SUB_OFF_ORIGIN_X, originX);
        putAddress(data, subOffset + SUB_OFF_ORIGIN_Y, originY);
        putAddress(data, subOffset + SUB_OFF_ORIGIN_Z, originZ);
        putAddress(data, subOffset + SUB_OFF_MUL_FACTOR, mulFactor);
        putInt(data + subOffset + SUB_OFF_LENGTH, length);
    }

    public static long createSimplexPermutation(int[] permutation) {
        long ptr = allocateAligned(permutation.length * 4L, 64);
        copyIntArray(ptr, permutation);
        return ptr;
    }
}
