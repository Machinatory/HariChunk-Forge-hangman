package org.admany.quantifiedintegration.opencl;

import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;

import org.lwjgl.PointerBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone OpenCL probe that runs in an isolated child JVM process.
 * If the OpenCL driver crashes (e.g. Intel iGPU heap corruption 0xc0000374),
 * only this process dies -- the parent game JVM is completely unaffected.
 *
 * Exit 0 = OpenCL available with GPU devices found
 * Exit 1 = OpenCL unavailable, crashed, or no suitable devices
 *
 * Output: single JSON line to stdout
 */
public final class OpenCLProbeEntrypoint {

    private OpenCLProbeEntrypoint() {
    }

    public static void main(String[] args) {
        try {
            CL.create();
            probe();
        } catch (Throwable t) {
            outputJson(false, t.getClass().getSimpleName() + ": " + t.getMessage(), List.of());
            System.exit(1);
        }
    }

    private static void probe() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer platformCount = stack.mallocInt(1);
            int err = CL10.clGetPlatformIDs(null, platformCount);
            if (err != CL10.CL_SUCCESS) {
                outputJson(false, "clGetPlatformIDs failed with error " + err, List.of());
                return;
            }
            int numPlatforms = platformCount.get(0);
            if (numPlatforms <= 0) {
                outputJson(false, "No OpenCL platforms found", List.of());
                return;
            }

            PointerBuffer platforms = stack.mallocPointer(numPlatforms);
            err = CL10.clGetPlatformIDs(platforms, (IntBuffer) null);
            if (err != CL10.CL_SUCCESS) {
                outputJson(false, "clGetPlatformIDs(list) failed with error " + err, List.of());
                return;
            }

            List<DeviceInfo> devices = new ArrayList<>();
            for (int i = 0; i < numPlatforms; i++) {
                try {
                    enumerateDevices(platforms.get(i), devices, stack);
                } catch (Throwable ignored) {
                }
            }

            if (devices.isEmpty()) {
                outputJson(false, "No OpenCL GPU devices found", List.of());
                return;
            }

            outputJson(true, null, devices);
        }
    }

    private static void enumerateDevices(long platformId, List<DeviceInfo> devices, MemoryStack stack) {
        IntBuffer deviceCount = stack.mallocInt(1);
        int err = CL10.clGetDeviceIDs(platformId, CL10.CL_DEVICE_TYPE_GPU, null, deviceCount);
        if (err != CL10.CL_SUCCESS || deviceCount.get(0) <= 0) {
            err = CL10.clGetDeviceIDs(platformId, CL10.CL_DEVICE_TYPE_ALL, null, deviceCount);
        }
        if (err != CL10.CL_SUCCESS || deviceCount.get(0) <= 0) {
            return;
        }

        int count = deviceCount.get(0);
        PointerBuffer deviceIds = stack.mallocPointer(count);
        err = CL10.clGetDeviceIDs(platformId, CL10.CL_DEVICE_TYPE_GPU, deviceIds, (IntBuffer) null);
        if (err != CL10.CL_SUCCESS) {
            CL10.clGetDeviceIDs(platformId, CL10.CL_DEVICE_TYPE_ALL, deviceIds, (IntBuffer) null);
        }

        for (int i = 0; i < count; i++) {
            long deviceId = deviceIds.get(i);
            try {
                String name = getInfoString(deviceId, CL10.CL_DEVICE_NAME, stack);
                String vendor = getInfoString(deviceId, CL10.CL_DEVICE_VENDOR, stack);
                long memSize = getInfoLong(deviceId, CL10.CL_DEVICE_GLOBAL_MEM_SIZE, stack);
                int cu = getInfoInt(deviceId, CL10.CL_DEVICE_MAX_COMPUTE_UNITS, stack);
                long typeFlags = getInfoLong(deviceId, CL10.CL_DEVICE_TYPE, stack);
                String typeName;
                if ((typeFlags & CL10.CL_DEVICE_TYPE_CPU) != 0) {
                    typeName = "CPU";
                } else if ((typeFlags & CL10.CL_DEVICE_TYPE_GPU) != 0) {
                    typeName = "GPU";
                } else {
                    typeName = "OTHER";
                }
                devices.add(new DeviceInfo(name, vendor, typeName, memSize, cu));
            } catch (Throwable ignored) {
            }
        }
    }

    private static String getInfoString(long deviceId, int param, MemoryStack stack) {
        PointerBuffer sizeBuf = stack.mallocPointer(1);
        CL10.clGetDeviceInfo(deviceId, param, (ByteBuffer) null, sizeBuf);
        int len = (int) sizeBuf.get(0);
        if (len <= 1) {
            return "";
        }
        ByteBuffer buf = stack.malloc(len);
        CL10.clGetDeviceInfo(deviceId, param, buf, null);
        byte[] bytes = new byte[len - 1];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int getInfoInt(long deviceId, int param, MemoryStack stack) {
        ByteBuffer buf = stack.malloc(4);
        CL10.clGetDeviceInfo(deviceId, param, buf, null);
        return buf.getInt(0);
    }

    private static long getInfoLong(long deviceId, int param, MemoryStack stack) {
        ByteBuffer buf = stack.malloc(8);
        CL10.clGetDeviceInfo(deviceId, param, buf, null);
        return buf.getLong(0);
    }

    private static void outputJson(boolean ok, String failure, List<DeviceInfo> devices) {
        StringBuilder sb = new StringBuilder("{\"ok\":");
        sb.append(ok);
        sb.append(",\"devices\":[");
        for (int i = 0; i < devices.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            DeviceInfo d = devices.get(i);
            sb.append("{\"name\":\"").append(escape(d.name)).append('"');
            sb.append(",\"vendor\":\"").append(escape(d.vendor)).append('"');
            sb.append(",\"type\":\"").append(d.type).append('"');
            sb.append(",\"globalMemSize\":").append(d.globalMemSize);
            sb.append(",\"computeUnits\":").append(d.computeUnits);
            sb.append('}');
        }
        sb.append(']');
        if (failure != null) {
            sb.append(",\"failure\":\"").append(escape(failure)).append('"');
        }
        sb.append('}');
        System.out.println(sb);
        System.out.flush();
        System.exit(ok ? 0 : 1);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private record DeviceInfo(String name, String vendor, String type, long globalMemSize, int computeUnits) {
    }
}
