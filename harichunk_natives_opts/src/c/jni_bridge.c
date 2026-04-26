#include <jni.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>

static size_t harichunk_strlen(const char *s) {
    size_t len = 0;
    while (s[len]) len++;
    return len;
}

static void harichunk_strcat(char *dst, const char *src, size_t max) {
    size_t dlen = harichunk_strlen(dst);
    size_t i = 0;
    while (src[i] && dlen + i < max - 1) {
        dst[dlen + i] = src[i];
        i++;
    }
    dst[dlen + i] = '\0';
}

#ifdef _WIN32
#include <windows.h>
static void *lookup_symbol(const char *name) {
    static HMODULE module = NULL;
    if (module == NULL) {
        /* Use our own DLL handle, not the main EXE */
        GetModuleHandleExA(
            GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
            (LPCSTR)lookup_symbol,
            &module
        );
    }
    return (void *)GetProcAddress(module, name);
}
#else
#include <dlfcn.h>
static void *lookup_symbol(const char *name) {
    return dlsym(RTLD_DEFAULT, name);
}
#endif

#include "includes/ext_math.h"
#include "includes/target_macros.h"

extern int32_t harichunk_natives_get_system_isa(_Bool allowAVX512);

typedef double (*fn_perlin_double)(const double_octave_sampler_data_t *, double, double, double);
typedef void (*fn_perlin_double_batch)(const double_octave_sampler_data_t *, double *, const double *, const double *, const double *, uint32_t);
typedef double (*fn_noise_interpolated)(const interpolated_noise_sampler_t *, double, double, double);
typedef float (*fn_end_islands_sample)(const uint32_t *, int32_t, int32_t);
typedef uint32_t (*fn_biome_access_sample)(int64_t, int32_t, int32_t, int32_t);

static struct {
    fn_perlin_double noise_perlin_double;
    fn_perlin_double_batch noise_perlin_double_batch;
    fn_noise_interpolated noise_interpolated;
    fn_end_islands_sample end_islands_sample;
    fn_biome_access_sample biome_access_sample;
} natives;

static const char *get_suffix_for_isa(int isa_level) {
    switch (isa_level) {
        case 9: return "_avx512spr";
        case 8: return "_avx512icl";
        case 7: return "_avx512skx";
        case 6: return "_avx2";
        case 5: return "_avx2adl";
        case 4: return "_avx2";
        case 3: return "_avx";
        case 2: return "_sse4_2";
        case 1: return "_sse2";
        case 0: default: return "_sse2";
    }
}

static void *lookup_with_suffix(const char *base, const char *suffix) {
    char full[256];
    full[0] = '\0';
    harichunk_strcat(full, base, sizeof(full));
    harichunk_strcat(full, suffix, sizeof(full));
    return lookup_symbol(full);
}

/* Forward declarations for JNI bridge functions */
JNIEXPORT jint JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_getSystemISA(JNIEnv *, jclass, jboolean);
JNIEXPORT jdouble JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_noisePerlinDouble(JNIEnv *, jclass, jlong, jdouble, jdouble, jdouble);
JNIEXPORT void JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_noisePerlinDoubleBatch(JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint);
JNIEXPORT jdouble JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_noiseInterpolated(JNIEnv *, jclass, jlong, jdouble, jdouble, jdouble);
JNIEXPORT jfloat JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_endIslandsSample(JNIEnv *, jclass, jlong, jint, jint);
JNIEXPORT jint JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_biomeAccessSample(JNIEnv *, jclass, jlong, jint, jint, jint);

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_8;
    }

    /* Resolve ISA-suffixed internal symbols */
    int isa = harichunk_natives_get_system_isa(1);
    const char *suffix = get_suffix_for_isa(isa);

    natives.noise_perlin_double = (fn_perlin_double)lookup_with_suffix("harichunk_natives_noise_perlin_double", suffix);
    natives.noise_perlin_double_batch = (fn_perlin_double_batch)lookup_with_suffix("harichunk_natives_noise_perlin_double_batch", suffix);
    natives.noise_interpolated = (fn_noise_interpolated)lookup_with_suffix("harichunk_natives_noise_interpolated", suffix);
    natives.end_islands_sample = (fn_end_islands_sample)lookup_with_suffix("harichunk_natives_end_islands_sample", suffix);
    natives.biome_access_sample = (fn_biome_access_sample)lookup_with_suffix("harichunk_natives_biome_access_sample", suffix);

    /* Explicitly register native methods to bypass classloader symbol lookup */
    jclass cls = (*env)->FindClass(env, "com/hari/harichunk/opts/natives_math/common/NativeBindings");
    if (cls != NULL) {
        static const JNINativeMethod methods[] = {
            {"getSystemISA",           "(Z)I",       (void *)Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_getSystemISA},
            {"noisePerlinDouble",      "(JDDD)D",    (void *)Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_noisePerlinDouble},
            {"noisePerlinDoubleBatch", "(JJJJJI)V",  (void *)Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_noisePerlinDoubleBatch},
            {"noiseInterpolated",      "(JDDD)D",    (void *)Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_noiseInterpolated},
            {"endIslandsSample",       "(JII)F",     (void *)Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_endIslandsSample},
            {"biomeAccessSample",      "(JIII)I",    (void *)Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_biomeAccessSample},
        };
        (*env)->RegisterNatives(env, cls, methods, 6);
    }

    return JNI_VERSION_1_8;
}

JNIEXPORT jint JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_getSystemISA
  (JNIEnv *env, jclass cls, jboolean allowAVX512) {
    return (jint)harichunk_natives_get_system_isa((_Bool)allowAVX512);
}

JNIEXPORT jdouble JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_noisePerlinDouble
  (JNIEnv *env, jclass cls, jlong dataPtr, jdouble x, jdouble y, jdouble z) {
    if (natives.noise_perlin_double == NULL) return 0.0;
    return natives.noise_perlin_double((const double_octave_sampler_data_t *)(intptr_t)dataPtr, x, y, z);
}

JNIEXPORT void JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_noisePerlinDoubleBatch
  (JNIEnv *env, jclass cls, jlong dataPtr, jlong resPtr, jlong xPtr, jlong yPtr, jlong zPtr, jint length) {
    if (natives.noise_perlin_double_batch == NULL) return;
    natives.noise_perlin_double_batch(
        (const double_octave_sampler_data_t *)(intptr_t)dataPtr,
        (double *)(intptr_t)resPtr,
        (const double *)(intptr_t)xPtr,
        (const double *)(intptr_t)yPtr,
        (const double *)(intptr_t)zPtr,
        (uint32_t)length
    );
}

JNIEXPORT jdouble JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_noiseInterpolated
  (JNIEnv *env, jclass cls, jlong dataPtr, jdouble x, jdouble y, jdouble z) {
    if (natives.noise_interpolated == NULL) return 0.0;
    return natives.noise_interpolated((const interpolated_noise_sampler_t *)(intptr_t)dataPtr, x, y, z);
}

JNIEXPORT jfloat JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_endIslandsSample
  (JNIEnv *env, jclass cls, jlong permPtr, jint x, jint z) {
    if (natives.end_islands_sample == NULL) return 0.0f;
    return natives.end_islands_sample((const uint32_t *)(intptr_t)permPtr, (int32_t)x, (int32_t)z);
}

JNIEXPORT jint JNICALL Java_com_hari_harichunk_opts_natives_math_common_NativeBindings_biomeAccessSample
  (JNIEnv *env, jclass cls, jlong seed, jint x, jint y, jint z) {
    if (natives.biome_access_sample == NULL) return 0;
    return (jint)natives.biome_access_sample((int64_t)seed, (int32_t)x, (int32_t)y, (int32_t)z);
}
