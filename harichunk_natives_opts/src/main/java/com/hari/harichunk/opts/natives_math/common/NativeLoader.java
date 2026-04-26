package com.hari.harichunk.opts.natives_math.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class NativeLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeLoader.class);

    public static final String NORMALIZED_ARCH = normalizeArch(System.getProperty("os.arch", ""));
    public static final String NORMALIZED_OS = normalizeOs(System.getProperty("os.name", ""));

    public static final ISATarget currentMachineTarget;
    public static final boolean available;

    static {
        boolean loaded = false;
        ISATarget target = null;
        try {
            String libName = String.format("%s-%s-%s", NORMALIZED_OS, NORMALIZED_ARCH,
                    System.mapLibraryName("harichunk-opts-natives-math"));
            loadFromResources(libName);
            loaded = true;
            LOGGER.info("Native library loaded, detecting ISA...");
            int level = NativeBindings.getSystemISA(true);
            Class<? extends Enum<? extends ISATarget>> isaClass = ISATarget.getInstance();
            if (isaClass != null) {
                Object[] constants = isaClass.getEnumConstants();
                if (level >= 0 && level < constants.length) {
                    target = (ISATarget) constants[level];
                    while (target != null && !target.isNativelySupported() && target.ordinal() > 0) {
                        target = (ISATarget) constants[target.ordinal() - 1];
                    }
                }
            }
            LOGGER.info("Detected maximum supported ISA target: {}", target);
        } catch (Throwable e) {
            if (e instanceof IOException && e.getMessage() != null
                && e.getMessage().startsWith("Cannot find native library")) {
                LOGGER.info("Native SIMD library not available ({}), using Java fallback. " +
                    "To enable native SIMD, compile the C sources in src/c/ and package the DLL/SO/DYLIB.", e.getMessage());
            } else {
                LOGGER.warn("Failed to load native library or detect ISA", e);
            }
            loaded = false;
            target = null;
        }
        available = loaded;
        currentMachineTarget = target;
    }

    public static String getAvailabilityString() {
        if (available) {
            return String.format("Available, with ISA target %s", currentMachineTarget);
        } else {
            return "Unavailable";
        }
    }

    private static void loadFromResources(String libName) throws IOException {
        try (InputStream in = NativeLoader.class.getClassLoader().getResourceAsStream(libName)) {
            if (in == null) {
                throw new IOException("Cannot find native library " + libName);
            }
            Path tempFile;
            if (Boolean.getBoolean("vectorizedgen.preserveNative")) {
                tempFile = Path.of(".", libName);
            } else {
                tempFile = Files.createTempFile(null, libName);
                tempFile.toFile().deleteOnExit();
            }
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.load(tempFile.toAbsolutePath().toString());
        }
    }

    static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) return "x86_64";
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) return "x86_32";
        if (value.matches("^(ia64|itanium64)$")) return "itanium_64";
        if (value.matches("^(aarch64)$")) return "aarch_64";
        if (value.matches("^(arm|arm32)$")) return "arm_32";
        if ("ppc64".equals(value)) return "ppc_64";
        if ("ppc64le".equals(value)) return "ppcle_64";
        if ("s390x".equals(value)) return "s390_64";
        if ("loongarch64".equals(value)) return "loongarch_64";
        return "unknown";
    }

    static String normalizeOs(String value) {
        value = normalize(value);
        if (value.startsWith("linux")) return "linux";
        if (value.startsWith("macosx") || value.startsWith("osx") || value.startsWith("darwin")) return "osx";
        if (value.startsWith("freebsd")) return "freebsd";
        if (value.startsWith("windows")) return "windows";
        return "unknown";
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }
}
