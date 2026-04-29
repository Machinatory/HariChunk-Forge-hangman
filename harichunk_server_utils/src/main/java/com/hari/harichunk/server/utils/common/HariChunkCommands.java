package com.hari.harichunk.server.utils.common;

import com.hari.harichunk.base.mixin.access.IServerChunkManager;
import com.hari.harichunk.notickvd.common.IChunkTicketManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HariChunkCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("harichunk")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("notick")
                                        .requires(unused -> com.hari.harichunk.notickvd.ModuleEntryPoint.enabled)
                                        .executes(HariChunkCommands::noTickCommand)
                        )
                        .then(
                                Commands.literal("debug")
                                        .requires(unused -> !FMLLoader.isProduction())
//                                        .then(
//                                                CommandManager.literal("mobcaps")
//                                                        .requires(unused -> com.hari.harichunk.notickvd.ModuleEntryPoint.enabled)
//                                                        .executes(HariChunkCommands::mobcapsCommand)
//                                        )
                        )
            .then(
                Commands.literal("data")
                    .executes(HariChunkCommands::dataCommand)
            )
            .then(
                Commands.literal("gpu")
                    .executes(HariChunkCommands::gpuCommand)
            )
        );
    }

    private static int noTickCommand(CommandContext<CommandSourceStack> ctx) {
        final ServerChunkCache chunkManager = ctx.getSource().getLevel().getLevel().getChunkSource();
        final DistanceManager ticketManager = ((IServerChunkManager) chunkManager).getTicketManager();
        final int noTickOnlyChunks = ((IChunkTicketManager) ticketManager).getNoTickOnlyChunks().size();
        final int noTickPendingTicketUpdates = ((IChunkTicketManager) ticketManager).getNoTickPendingTicketUpdates();
        ctx.getSource().sendSuccess(() -> Component.nullToEmpty(String.format("No-tick chunks: %d", noTickOnlyChunks)), true);
        ctx.getSource().sendSuccess(() -> Component.nullToEmpty(String.format("No-tick chunk pending ticket updates: %d", noTickPendingTicketUpdates)), true);

        return 0;
    }

    private static int dataCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        sendLine(source, "HariChunk Runtime Data");
        sendLine(source, "Environment: forge=" + FMLLoader.versionInfo().forgeVersion() + " production=" + FMLLoader.isProduction());
        sendLine(source, "Commands: /quantified exists only when mod 'quantified' is loaded and the server command tree has been built; some subcommands are admin-gated.");

        boolean quantifiedLoaded = ModList.get().isLoaded("quantified");
        boolean quantifiedBridgeLoaded = ModList.get().isLoaded("harichunk_x_quantified_api");
        boolean quantifiedRegistered = invokeStaticBoolean("org.admany.quantifiedintegration.QuantifiedIntegration", "isAvailable", false);
        sendLine(source, "Quantified API: modLoaded=" + quantifiedLoaded
                + " bridgeLoaded=" + quantifiedBridgeLoaded
                + " bridgeRegistered=" + quantifiedRegistered);

        boolean dagLoaded = ModList.get().isLoaded("quantified_admany_dag_scheduler");
        boolean dagUsingQuantified = invokeStaticBoolean("org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler", "isUsingQuantified", false);
        String dagStats = invokeStaticString("org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler", "getStatsString", "unavailable");
        sendLine(source, "DAG scheduler: modLoaded=" + dagLoaded + " usingQuantified=" + dagUsingQuantified + " stats=" + dagStats);

        boolean gpuNoiseLoaded = ModList.get().isLoaded("harichunk_opts_gpu_noise");
        boolean gpuNoiseEnabled = readStaticBoolean("com.hari.harichunk.opts.gpu_noise.ModuleEntryPoint", "enabled", false);
        String gpuBackend = invokeStaticString("com.hari.harichunk.opts.gpu_noise.common.GpuNoiseBackend", "getStatusString", "unavailable");
        String gpuBackendType = invokeStaticString("com.hari.harichunk.opts.gpu_noise.common.GpuNoiseBackend", "getActiveBackend", "unknown");
        sendLine(source, "GPU noise: modLoaded=" + gpuNoiseLoaded + " enabled=" + gpuNoiseEnabled + " backend=" + gpuBackendType + " status=" + gpuBackend);

        boolean vkAccelLoaded = ModList.get().isLoaded("quantified_harichunk_opts_vk_gpu_accel");
        boolean vkReady = invokeStaticBoolean("org.admany.vkgpuaccel.VkGpuAccel", "isReady", false);
        boolean vkOwnsNoise = invokeStaticBoolean("org.admany.vkgpuaccel.VkGpuAccel", "shouldOwnGpuNoise", false);
        boolean vkExactDensity = invokeStaticBoolean("org.admany.vkgpuaccel.VkGpuAccel", "canComputeDensityBatches", false);
        String vkStatus = invokeStaticString("org.admany.vkgpuaccel.VkGpuAccel", "statusString", "unavailable");
        sendLine(source, "Quantified Vulkan accel: modLoaded=" + vkAccelLoaded + " ready=" + vkReady + " ownsGpuNoise=" + vkOwnsNoise + " exactDensity=" + vkExactDensity + " status=" + vkStatus);

        boolean brsLoaded = ModList.get().isLoaded("harichunk_brs_gpu_noise");
        boolean brsReady = invokeStaticBoolean("org.admany.brsgpunoise.BrsGpuNoise", "isReady", false);
        String brsStatus = invokeStaticString("org.admany.brsgpunoise.BrsGpuNoise", "statusString", "unavailable");
        sendLine(source, "BRS GPU noise: modLoaded=" + brsLoaded + " ready=" + brsReady + " status=" + brsStatus);

        boolean dfcLoaded = ModList.get().isLoaded("harichunk_opts_dfc");
        boolean dfcEnabled = readStaticBoolean("com.hari.harichunk.opts.dfc.ModuleEntryPoint", "enabled", false);
        boolean dfcGpuEnabled = readStaticBoolean("com.hari.harichunk.opts.dfc.ModuleEntryPoint", "gpuAccelerationEnabled", false);
        boolean dfcGpuAvailable = invokeStaticBoolean("com.hari.harichunk.opts.dfc.common.gen.GpuDensityFunction", "isGpuAvailable", false);
        sendLine(source, "DFC: modLoaded=" + dfcLoaded + " compilerEnabled=" + dfcEnabled + " gpuFlag=" + dfcGpuEnabled + " gpuAvailable=" + dfcGpuAvailable);

        List<String> missingMods = new ArrayList<>();
        if (!quantifiedLoaded) missingMods.add("quantified");
        if (!quantifiedBridgeLoaded) missingMods.add("harichunk_x_quantified_api");
        if (!dagLoaded) missingMods.add("quantified_admany_dag_scheduler");
        if (!vkAccelLoaded) missingMods.add("quantified_harichunk_opts_vk_gpu_accel");
        if (!brsLoaded) missingMods.add("harichunk_brs_gpu_noise");
        if (!gpuNoiseLoaded) missingMods.add("harichunk_opts_gpu_noise");
        if (!dfcLoaded) missingMods.add("harichunk_opts_dfc");

        if (missingMods.isEmpty()) {
            sendLine(source, "Missing runtime pieces: none");
        } else {
            sendLine(source, "Missing runtime pieces: " + String.join(", ", missingMods));
        }

        return 1;
    }

        private static int gpuCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        sendLine(source, "HariChunk GPU Runtime");
        sendLine(source, "GPU noise backend: " + invokeStaticString(
            "com.hari.harichunk.opts.gpu_noise.common.GpuNoiseBackend",
            "getStatusString",
            "unavailable"));
        sendLine(source, "Quantified Vulkan accel: " + invokeStaticString(
            "org.admany.vkgpuaccel.VkGpuAccel",
            "debugString",
            "unavailable"));
        sendLine(source, "BRS GPU noise: " + invokeStaticString(
            "org.admany.brsgpunoise.BrsGpuNoise",
            "debugString",
            "unavailable"));
        sendLine(source, "DFC GPU: " + invokeStaticString(
            "com.hari.harichunk.opts.dfc.common.gen.GpuDensityFunction",
            "debugString",
            "unavailable"));
        sendLine(source, "DAG scheduler: " + invokeStaticString(
            "org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler",
            "getStatsString",
            "unavailable"));

        return 1;
        }

    private static void sendLine(CommandSourceStack source, String line) {
        source.sendSuccess(() -> Component.nullToEmpty(line), false);
    }

    private static boolean readStaticBoolean(String className, String fieldName, boolean fallback) {
        try {
            Class<?> type = Class.forName(className);
            Field field = type.getField(fieldName);
            return field.getBoolean(null);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean invokeStaticBoolean(String className, String methodName, boolean fallback) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod(methodName);
            Object value = method.invoke(null);
            return value instanceof Boolean bool ? bool : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String invokeStaticString(String className, String methodName, String fallback) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod(methodName);
            Object value = method.invoke(null);
            return value == null ? fallback : value.toString();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

//    private static int mobcapsCommand(CommandContext<ServerCommandSource> ctx) {
//        final ServerWorld serverWorld = ctx.getSource().getWorld().toServerWorld();
//        final ServerChunkManager chunkManager = serverWorld.getChunkManager();
//        final ChunkTicketManager ticketManager = ((IServerChunkManager) chunkManager).getTicketManager();
//        final LongSet noTickOnlyChunks = ((IChunkTicketManager) ticketManager).getNoTickOnlyChunks();
//        final Iterable<Entity> iterable;
//        if (noTickOnlyChunks == null) {
//            iterable = serverWorld.iterateEntities();
//        } else {
//            iterable = new FilteringIterable<>(serverWorld.iterateEntities(), entity -> !noTickOnlyChunks.contains(entity.getChunkPos().toLong()));
//        }
//
//        ctx.getSource().sendFeedback(Text.of("Mobcap details"), true);
//        for (Entity entity : iterable) {
//            if (entity instanceof MobEntity mobEntity) {
//                ctx.getSource().sendFeedback(Text.of(String.format("%s: ", mobEntity.getType().getSpawnGroup().asString())).(mobEntity.getDisplayName()).append(String.format(" in %s", mobEntity.getChunkPos())), true);
//            }
//        }
//        return 0;
//    }

}
