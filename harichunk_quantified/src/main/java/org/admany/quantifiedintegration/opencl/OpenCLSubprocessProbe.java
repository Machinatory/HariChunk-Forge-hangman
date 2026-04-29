package org.admany.quantifiedintegration.opencl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/**
 * Launches an isolated child JVM to probe OpenCL availability.
 * If the child crashes (e.g. Intel iGPU driver heap corruption 0xc0000374),
 * the parent JVM is completely unaffected.
 *
 * The child JVM loads LWJGL OpenCL native libraries and calls CL.create() /
 * clGetPlatformIDs / clGetDeviceIDs. If the native driver corrupts the heap,
 * only the child dies.
 */
final class OpenCLSubprocessProbe {

    private static final String ENTRYPOINT = "org.admany.quantifiedintegration.opencl.OpenCLProbeEntrypoint";
    private static final long TIMEOUT_SECONDS = 15L;

    private OpenCLSubprocessProbe() {
    }

    static Result run(Logger logger) {
        try {
            String javaExe = ProcessHandle.current().info().command()
                    .orElseGet(() -> System.getProperty("java.home") + File.separator + "bin"
                            + File.separator + "java");

            String classpath = System.getProperty("java.class.path");
            if (classpath == null || classpath.isBlank()) {
                return Result.failed("No classpath available for subprocess");
            }

            String lwjglPath = System.getProperty("org.lwjgl.librarypath");

            List<String> command = new ArrayList<>();
            command.add(javaExe);
            command.add("-Xms16m");
            command.add("-Xmx128m");
            command.add("-XX:MaxDirectMemorySize=64m");
            command.add("-Xss4m");
            if (lwjglPath != null && !lwjglPath.isBlank()) {
                command.add("-Dorg.lwjgl.librarypath=" + lwjglPath);
                command.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + lwjglPath);
            }
            addProp(command, "java.library.path");
            command.add("-cp");
            command.add(classpath);
            command.add(ENTRYPOINT);

            logger.info("[HariChunk] Launching OpenCL safety probe subprocess");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(3, TimeUnit.SECONDS);
                String lastLine = lines.isEmpty() ? "<none>" : lines.get(lines.size() - 1);
                logger.warn("[HariChunk] OpenCL probe subprocess timed out after {}s; last output: {}",
                        TIMEOUT_SECONDS, lastLine);
                return Result.failed("OpenCL probe subprocess timed out (last: " + lastLine + ")");
            }

            int exitCode = process.exitValue();
            String output = String.join("\n", lines);
            return parseResult(exitCode, output, logger);
        } catch (IOException e) {
            return Result.failed("Failed to launch OpenCL probe subprocess: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failed("OpenCL probe subprocess interrupted");
        } catch (Throwable t) {
            return Result.failed("Unexpected OpenCL probe failure: " + t.getMessage());
        }
    }

    private static void addProp(List<String> command, String key) {
        String val = System.getProperty(key);
        if (val != null && !val.isBlank()) {
            command.add("-D" + key + "=" + val);
        }
    }

    private static Result parseResult(int exitCode, String output, Logger logger) {
        String json = extractJson(output);
        if (json == null) {
            logger.warn("[HariChunk] OpenCL probe: no JSON output (exit={}, output={})",
                    exitCode, truncate(output, 200));
            return Result.failed("No JSON output from probe (exit=" + exitCode + ")");
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            boolean ok = root.has("ok") && root.get("ok").getAsBoolean();
            List<DeviceResult> devices = new ArrayList<>();
            if (root.has("devices")) {
                JsonArray arr = root.getAsJsonArray("devices");
                for (JsonElement e : arr) {
                    if (!e.isJsonObject()) {
                        continue;
                    }
                    JsonObject d = e.getAsJsonObject();
                    devices.add(new DeviceResult(
                            getString(d, "name", ""),
                            getString(d, "vendor", ""),
                            getString(d, "type", ""),
                            getLong(d, "globalMemSize", 0),
                            getInt(d, "computeUnits", 0)
                    ));
                }
            }
            if (ok) {
                logger.info("[HariChunk] OpenCL probe succeeded with {} device(s)", devices.size());
                return Result.success(devices);
            }
            String failure = getString(root, "failure", "OpenCL probe failed");
            logger.warn("[HariChunk] OpenCL probe failed: {}", failure);
            return Result.failed(failure);
        } catch (Throwable t) {
            logger.warn("[HariChunk] Failed to parse probe JSON: {}", t.getMessage());
            return Result.failed("Failed to parse probe output: " + t.getMessage());
        }
    }

    private static String extractJson(String output) {
        if (output == null || output.isBlank()) {
            return null;
        }
        String trimmed = output.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        String[] lines = trimmed.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                return line;
            }
        }
        return null;
    }

    private static String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static long getLong(JsonObject obj, String key, long fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsLong();
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "...";
    }

    static final class Result {
        final boolean ok;
        final String failureReason;
        final List<DeviceResult> devices;

        Result(boolean ok, String failureReason, List<DeviceResult> devices) {
            this.ok = ok;
            this.failureReason = failureReason;
            this.devices = devices != null ? List.copyOf(devices) : List.of();
        }

        static Result success(List<DeviceResult> devices) {
            return new Result(true, null, devices);
        }

        static Result failed(String reason) {
            return new Result(false, reason, List.of());
        }
    }

    static final class DeviceResult {
        final String name;
        final String vendor;
        final String type;
        final long globalMemSize;
        final int computeUnits;

        DeviceResult(String name, String vendor, String type, long globalMemSize, int computeUnits) {
            this.name = name;
            this.vendor = vendor;
            this.type = type;
            this.globalMemSize = globalMemSize;
            this.computeUnits = computeUnits;
        }
    }
}
