package com.hari.harichunk.base.common.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Utility class to invoke GenerationTask.doWork() using reflection.
 * Designed to work with both Fabric (Yarn mappings) and Forge via Sinytra
 * Connector (SRG mappings).
 */
public class GenerationTaskInvoker {

    private static final Method DO_WORK_METHOD;

    static {
        Method method = null;

        // Get the GenerationTask interface class from ChunkStatus
        Class<?> generationTaskClass = null;
        for (Class<?> innerClass : ChunkStatus.class.getDeclaredClasses()) {
            String simpleName = innerClass.getSimpleName();
            // Match both Yarn and potential obfuscated names
            if (simpleName.equals("GenerationTask") || innerClass.isInterface()) {
                // Verify it's an interface with a method returning CompletableFuture
                if (innerClass.isInterface()) {
                    for (Method m : innerClass.getDeclaredMethods()) {
                        if (m.getReturnType() == CompletableFuture.class && m.getParameterCount() == 9) {
                            generationTaskClass = innerClass;
                            break;
                        }
                    }
                }
                if (generationTaskClass != null)
                    break;
            }
        }

        if (generationTaskClass == null) {
            // Fallback: search all inner interfaces for one with matching signature
            System.out.println("[HariChunk] GenerationTask not found by name, searching all interfaces...");
            for (Class<?> innerClass : ChunkStatus.class.getDeclaredClasses()) {
                if (innerClass.isInterface()) {
                    for (Method m : innerClass.getDeclaredMethods()) {
                        if (m.getReturnType() == CompletableFuture.class &&
                                m.getParameterCount() == 9 &&
                                !Modifier.isStatic(m.getModifiers())) {
                            generationTaskClass = innerClass;
                            System.out.println("[HariChunk] Found matching interface: " + innerClass.getName());
                            break;
                        }
                    }
                    if (generationTaskClass != null)
                        break;
                }
            }
        }

        if (generationTaskClass == null) {
            throw new RuntimeException("[HariChunk] Failed to find ChunkStatus.GenerationTask interface!");
        }

        System.out.println("[HariChunk] Using GenerationTask class: " + generationTaskClass.getName());

        // Find the doWork method - it's the only abstract method with 9 params
        // returning CompletableFuture
        // This approach is mapping-agnostic
        for (Method m : generationTaskClass.getDeclaredMethods()) {
            // Check: returns CompletableFuture, has 9 parameters, is abstract (not
            // default/static)
            if (m.getReturnType() == CompletableFuture.class &&
                    m.getParameterCount() == 9 &&
                    !Modifier.isStatic(m.getModifiers()) &&
                    !m.isDefault()) {
                method = m;
                method.setAccessible(true);
                System.out.println("[HariChunk] Found GenerationTask method: " + m.getName() +
                        " with params: " + m.getParameterCount());
                break;
            }
        }

        if (method == null) {
            // Debug: print all methods in the interface
            System.err.println("[HariChunk] DEBUG: All methods in " + generationTaskClass.getName() + ":");
            for (Method m : generationTaskClass.getDeclaredMethods()) {
                System.err.println("[HariChunk]   - " + m.getName() + " params=" + m.getParameterCount() +
                        " returns=" + m.getReturnType().getSimpleName() +
                        " static=" + Modifier.isStatic(m.getModifiers()) +
                        " default=" + m.isDefault());
            }
            throw new RuntimeException(
                    "[HariChunk] Failed to find GenerationTask.doWork method! This is likely a mapping issue.");
        }

        DO_WORK_METHOD = method;
    }

    /**
     * Invokes the doWork method on a GenerationTask instance.
     * This method handles different naming conventions across Fabric/Forge
     * environments.
     *
     * @param task The GenerationTask instance
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> invokeDoWork(
            Object task,
            ChunkStatus status,
            Executor executor,
            ServerLevel world,
            ChunkGenerator generator,
            StructureTemplateManager structureManager,
            ThreadedLevelLightEngine lightEngine,
            Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> func,
            List<ChunkAccess> chunks,
            ChunkAccess chunk) {
        try {
            return (CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>) DO_WORK_METHOD.invoke(
                    task, status, executor, world, generator, structureManager, lightEngine, func, chunks, chunk);
        } catch (Throwable e) {
            // Check for Industrial Upgrade crash
            Throwable cause = e;
            boolean isIndustrialUpgradeCrash = false;
            while (cause != null) {
                for (StackTraceElement element : cause.getStackTrace()) {
                    if (element.getClassName().contains("com.denfop.world.vein.AlgorithmVein")) {
                        isIndustrialUpgradeCrash = true;
                        break;
                    }
                }
                if (isIndustrialUpgradeCrash)
                    break;
                cause = cause.getCause();
            }

            if (isIndustrialUpgradeCrash) {
                System.err.println(
                        "[HariChunk] SUPPRESSED CRASH: Industrial Upgrade NullPointerException in AlgorithmVein detected.");
                System.err.println("[HariChunk] This vein will not generate, but the server will continue running.");
                e.printStackTrace(); // Still print it so user knows
                return CompletableFuture.completedFuture(Either.left(chunk));
            }

            // Provide detailed error info for debugging
            System.err.println("[HariChunk] Failed to invoke GenerationTask.doWork");
            System.err.println("[HariChunk] Method: " + DO_WORK_METHOD);
            System.err.println("[HariChunk] Task type: " + (task != null ? task.getClass().getName() : "null"));
            throw new RuntimeException("[HariChunk] Failed to invoke GenerationTask.doWork", e);
        }
    }
}
