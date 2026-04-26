package com.hari.harichunk.opts.gpu_noise.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OpenCLManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("HariChunk GPU Noise/OpenCL");

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static volatile boolean available = false;

    // Reflected CL10 methods - null if OpenCL unavailable
    private static Method clGetPlatformIDs;
    private static Method clGetDeviceIDs;
    private static Method clCreateContext;
    private static Method clCreateCommandQueue;
    private static Method clCreateProgramWithSource;
    private static Method clBuildProgram;
    private static Method clCreateKernel;
    private static Method clCreateBuffer;
    private static Method clSetKernelArg1p;
    private static Method clSetKernelArg1d;
    private static Method clSetKernelArg1i;
    private static Method clEnqueueNDRangeKernel;
    private static Method clEnqueueReadBuffer;
    private static Method clFinish;
    private static Method clReleaseMemObject;
    private static Method clReleaseKernel;
    private static Method clReleaseProgram;
    private static Method clReleaseCommandQueue;
    private static Method clReleaseContext;
    private static Method clGetProgramBuildInfo;

    private static Object clContext;
    private static Object clCommandQueue;
    private static long clCommandQueueId;
    private static long clPerlinKernel;
    private static long clDevice;

    private static final int CL_DEVICE_TYPE_GPU = (1 << 2);
    private static final int CL_DEVICE_TYPE_ALL = 0xFFFFFFFF;
    private static final int CL_MEM_READ_ONLY = (1 << 2);
    private static final int CL_MEM_WRITE_ONLY = (1 << 3);
    private static final int CL_MEM_COPY_HOST_PTR = (1 << 5);
    private static final int CL_SUCCESS = 0;
    private static final int CL_PROGRAM_BUILD_LOG = 0x1083;

    private OpenCLManager() {
    }

    public static synchronized void initialize() {
        if (!initialized.compareAndSet(false, true)) return;

        try {
            Class<?> cl10Class = Class.forName("org.lwjgl.opencl.CL10");
            reflectMethods(cl10Class);
            LOGGER.info("LWJGL OpenCL found, attempting GPU initialization...");
            doInitialize();
        } catch (ClassNotFoundException e) {
            LOGGER.info("LWJGL OpenCL not available, GPU noise disabled (CPU fallback active)");
        } catch (NoSuchMethodException e) {
            LOGGER.info("LWJGL OpenCL API mismatch, GPU noise disabled: {}", e.getMessage());
        } catch (Throwable t) {
            LOGGER.warn("OpenCL initialization failed: {}", t.getMessage());
        }
    }

    private static void reflectMethods(Class<?> cl10Class) throws NoSuchMethodException {
        clGetPlatformIDs = cl10Class.getMethod("clGetPlatformIDs", LongBuffer.class, IntBuffer.class);
        clGetDeviceIDs = cl10Class.getMethod("clGetDeviceIDs", long.class, long.class, LongBuffer.class, IntBuffer.class);
        clCreateContext = cl10Class.getMethod("clCreateContext", long.class, LongBuffer.class, IntBuffer.class);
        clCreateCommandQueue = cl10Class.getMethod("clCreateCommandQueue", long.class, long.class, long.class, IntBuffer.class);
        clCreateProgramWithSource = cl10Class.getMethod("clCreateProgramWithSource", long.class, ByteBuffer.class, IntBuffer.class);
        clBuildProgram = cl10Class.getMethod("clBuildProgram", long.class, long.class, CharSequence.class, long.class, long.class);
        clCreateKernel = cl10Class.getMethod("clCreateKernel", long.class, CharSequence.class, IntBuffer.class);
        clCreateBuffer = cl10Class.getMethod("clCreateBuffer", long.class, long.class, long.class, ByteBuffer.class, IntBuffer.class);
        clSetKernelArg1p = cl10Class.getMethod("clSetKernelArg1p", long.class, int.class, long.class);
        clSetKernelArg1d = cl10Class.getMethod("clSetKernelArg1d", long.class, int.class, double.class);
        clSetKernelArg1i = cl10Class.getMethod("clSetKernelArg1i", long.class, int.class, int.class);
        clEnqueueNDRangeKernel = cl10Class.getMethod("clEnqueueNDRangeKernel", long.class, long.class, int.class, LongBuffer.class, LongBuffer.class, LongBuffer.class, long.class, long.class);
        clEnqueueReadBuffer = cl10Class.getMethod("clEnqueueReadBuffer", long.class, long.class, boolean.class, long.class, ByteBuffer.class, LongBuffer.class, LongBuffer.class);
        clFinish = cl10Class.getMethod("clFinish", long.class);
        clReleaseMemObject = cl10Class.getMethod("clReleaseMemObject", long.class);
        clReleaseKernel = cl10Class.getMethod("clReleaseKernel", long.class);
        clReleaseProgram = cl10Class.getMethod("clReleaseProgram", long.class);
        clReleaseCommandQueue = cl10Class.getMethod("clReleaseCommandQueue", long.class);
        clReleaseContext = cl10Class.getMethod("clReleaseContext", long.class);
        clGetProgramBuildInfo = cl10Class.getMethod("clGetProgramBuildInfo", long.class, long.class, int.class, ByteBuffer.class, IntBuffer.class);
    }

    @SuppressWarnings("unchecked")
    private static void doInitialize() throws Exception {
        java.nio.LongBuffer platforms = java.nio.LongBuffer.allocate(16);
        clGetPlatformIDs.invoke(null, platforms, null);
        if (platforms.remaining() == 0 || platforms.get(0) == 0) {
            LOGGER.info("No OpenCL platforms found");
            return;
        }
        long platform = platforms.get(0);

        java.nio.LongBuffer devices = java.nio.LongBuffer.allocate(16);
        clGetDeviceIDs.invoke(null, platform, (long) CL_DEVICE_TYPE_GPU, devices, null);
        if (devices.remaining() == 0 || devices.get(0) == 0) {
            clGetDeviceIDs.invoke(null, platform, (long) CL_DEVICE_TYPE_ALL, devices, null);
        }
        if (devices.remaining() == 0 || devices.get(0) == 0) {
            LOGGER.info("No OpenCL devices found");
            return;
        }
        clDevice = devices.get(0);

        IntBuffer errCode = ByteBuffer.allocateDirect(4).order(java.nio.ByteOrder.nativeOrder()).asIntBuffer();
        long ctx = (long) clCreateContext.invoke(null, platform, null, errCode);
        if (errCode.get(0) != CL_SUCCESS) {
            LOGGER.warn("clCreateContext failed: {}", errCode.get(0));
            return;
        }
        clContext = ctx;

        long queue = (long) clCreateCommandQueue.invoke(null, ctx, clDevice, 0L, errCode);
        if (errCode.get(0) != CL_SUCCESS) {
            LOGGER.warn("clCreateCommandQueue failed: {}", errCode.get(0));
            clReleaseContext.invoke(null, ctx);
            return;
        }
        clCommandQueueId = queue;

        String kernelSource = loadKernelSource("/assets/harichunk/gpu_kernels/perlin_noise.cl");
        if (kernelSource == null) {
            LOGGER.warn("Could not load Perlin noise OpenCL kernel");
            cleanup();
            return;
        }

        ByteBuffer sourceBuf = java.nio.ByteBuffer.allocateDirect(kernelSource.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1);
        sourceBuf.put(kernelSource.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        sourceBuf.flip();

        long program = (long) clCreateProgramWithSource.invoke(null, ctx, sourceBuf, errCode);
        if (errCode.get(0) != CL_SUCCESS) {
            LOGGER.warn("clCreateProgramWithSource failed: {}", errCode.get(0));
            cleanup();
            return;
        }

        int buildErr = (int) clBuildProgram.invoke(null, program, clDevice, "", 0L, 0L);
        if (buildErr != CL_SUCCESS) {
            ByteBuffer buildLog = ByteBuffer.allocateDirect(4096);
            clGetProgramBuildInfo.invoke(null, program, clDevice, CL_PROGRAM_BUILD_LOG, buildLog, null);
            LOGGER.error("OpenCL kernel build failed (error {})", buildErr);
            clReleaseProgram.invoke(null, program);
            cleanup();
            return;
        }

        long kernel = (long) clCreateKernel.invoke(null, program, "perlin_noise_batch", errCode);
        if (errCode.get(0) != CL_SUCCESS) {
            LOGGER.warn("clCreateKernel failed: {}", errCode.get(0));
            clReleaseProgram.invoke(null, program);
            cleanup();
            return;
        }
        clPerlinKernel = kernel;
        clReleaseProgram.invoke(null, program);

        available = true;
        LOGGER.info("OpenCL GPU noise acceleration initialized successfully");
    }

    public static boolean isAvailable() {
        return available;
    }

    public static String getAvailabilityString() {
        if (available) return "OpenCL GPU acceleration active";
        return "GPU acceleration unavailable (using CPU fallback)";
    }

    private static String loadKernelSource(String path) {
        try (java.io.InputStream is = OpenCLManager.class.getResourceAsStream(path)) {
            if (is == null) return null;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                return reader.lines().collect(java.util.stream.Collectors.joining("\n"));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load kernel source: {}", e.getMessage());
            return null;
        }
    }

    private static void cleanup() {
        available = false;
        try { if (clPerlinKernel != 0 && clReleaseKernel != null) clReleaseKernel.invoke(null, clPerlinKernel); } catch (Throwable ignored) {}
        try { if (clCommandQueueId != 0 && clReleaseCommandQueue != null) clReleaseCommandQueue.invoke(null, clCommandQueueId); } catch (Throwable ignored) {}
        try { if (clContext != null && clReleaseContext != null) clReleaseContext.invoke(null, clContext); } catch (Throwable ignored) {}
    }
}
