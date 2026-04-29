package org.admany.quantifiedintegration.opencl;

import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Intercepts QAPI's in-process OpenCL probe to prevent native heap corruption crashes.
 *
 * <p>Problem: QAPI's {@code AsyncProbeScheduler} runs OpenCL probing (CL.create() +
 * clGetPlatformIDs) directly in the game JVM. On some Intel iGPU drivers this causes
 * STATUS_HEAP_CORRUPTION (0xc0000374), killing the entire game process.</p>
 *
 * <p>Solution:</p>
 * <ol>
 *   <li>Immediately block QAPI's in-process probe via reflection on AsyncProbeScheduler</li>
 *   <li>Run our own OpenCL probe in an isolated child JVM (subprocess)</li>
 *   <li>If the subprocess probe succeeds (driver is safe), restore QAPI's probe so
 *       OpenCL features work normally</li>
 *   <li>If the subprocess crashes or fails (driver is broken), permanently disable
 *       in-process OpenCL while keeping Vulkan and CPU compute fully active</li>
 * </ol>
 *
 * <p>This runs entirely in HariChunk code — no QAPI modifications needed.</p>
 */
public final class OpenCLSafetyInterceptor {

    private static final String SCHEDULER_CLASS = "org.admany.quantified.core.common.opencl.gpu.AsyncProbeScheduler";
    private static final String RUNTIME_CLASS = "org.admany.quantified.core.common.opencl.core.OpenCLRuntime";

    private static volatile boolean intercepted = false;

    private OpenCLSafetyInterceptor() {
    }

    /**
     * Called once during mod construction. Safe to call multiple times (no-op after first).
     */
    public static void intercept(Logger logger) {
        if (intercepted) {
            return;
        }
        intercepted = true;

        boolean prevented = preventQapiProbe(logger);
        if (prevented) {
            runSubprocessVerification(logger);
        }
    }

    /**
     * Prevents QAPI's AsyncProbeScheduler from running its in-process OpenCL probe.
     * Sets {@code succeeded = true} and {@code remainingAttempts = 0} via reflection,
     * which causes all probe scheduling and execution to short-circuit.
     */
    private static boolean preventQapiProbe(Logger logger) {
        try {
            Class<?> schedulerClass = Class.forName(SCHEDULER_CLASS);

            setBooleanField(schedulerClass, "succeeded", true);

            Field attemptsField = schedulerClass.getDeclaredField("remainingAttempts");
            attemptsField.setAccessible(true);
            ((AtomicInteger) attemptsField.get(null)).set(0);

            logger.info("[HariChunk] OpenCL safety: prevented QAPI in-process probe "
                    + "(subprocess verification pending)");
            return true;
        } catch (ClassNotFoundException e) {
            logger.debug("[HariChunk] OpenCL safety: QAPI not present, nothing to intercept");
            return false;
        } catch (Throwable t) {
            logger.warn("[HariChunk] OpenCL safety: failed to prevent QAPI probe: {}", t.toString());
            return false;
        }
    }

    /**
     * Runs the subprocess probe asynchronously. When it completes, either restores
     * QAPI's probe (driver works) or locks down OpenCL (driver crashes).
     */
    private static void runSubprocessVerification(Logger logger) {
        CompletableFuture.runAsync(() -> {
            try {
                OpenCLSubprocessProbe.Result result = OpenCLSubprocessProbe.run(logger);
                if (result.ok) {
                    restoreQapiProbe(logger);
                } else {
                    lockdownOpenCL(logger, result.failureReason);
                }
            } catch (Throwable t) {
                logger.warn("[HariChunk] OpenCL safety: subprocess verification error: {}", t.toString());
                lockdownOpenCL(logger, "subprocess error: " + t.getMessage());
            }
        });
    }

    /**
     * OpenCL driver works — restore QAPI's probe flags and trigger a re-probe.
     * QAPI will run its normal in-process CL.create() which is now proven safe.
     */
    private static void restoreQapiProbe(Logger logger) {
        try {
            Class<?> schedulerClass = Class.forName(SCHEDULER_CLASS);

            setBooleanField(schedulerClass, "succeeded", false);

            Field attemptsField = schedulerClass.getDeclaredField("remainingAttempts");
            attemptsField.setAccessible(true);
            ((AtomicInteger) attemptsField.get(null)).set(6);

            setBooleanField(schedulerClass, "scheduled", false);

            try {
                Method triggerMethod = schedulerClass.getMethod("triggerProbe", String.class);
                triggerMethod.invoke(null, "harichunk-subprocess-verified");
            } catch (Throwable t) {
                logger.debug("[HariChunk] OpenCL safety: could not trigger QAPI re-probe: {}",
                        t.toString());
            }

            logger.info("[HariChunk] OpenCL safety: subprocess probe succeeded, "
                    + "QAPI in-process probe restored");
        } catch (Throwable t) {
            logger.debug("[HariChunk] OpenCL safety: could not restore QAPI probe: {}",
                    t.toString());
        }
    }

    /**
     * OpenCL driver is broken — permanently prevent in-process CL.create().
     * Sets OpenCLRuntime.INITIALISED = true so ensureInitialised() returns immediately
     * without touching the native driver.
     *
     * Vulkan acceleration and CPU compute remain fully active.
     */
    private static void lockdownOpenCL(Logger logger, String reason) {
        try {
            Class<?> runtimeClass = Class.forName(RUNTIME_CLASS);
            Field initialisedField = runtimeClass.getDeclaredField("INITIALISED");
            initialisedField.setAccessible(true);
            ((AtomicBoolean) initialisedField.get(null)).set(true);

            logger.info("[HariChunk] OpenCL safety: locked down in-process OpenCL "
                    + "(reason: {})", reason);
            logger.info("[HariChunk] OpenCL safety: Vulkan acceleration and CPU compute "
                    + "remain fully active");
        } catch (Throwable t) {
            logger.debug("[HariChunk] OpenCL safety: could not lockdown OpenCL runtime: {}",
                    t.toString());
        }
    }

    private static void setBooleanField(Class<?> clazz, String fieldName, boolean value)
            throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(null, value);
    }
}
