package com.hari.harichunk;

import com.ibm.asyncutil.util.Combinators;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Mod("harichunk")
public class HariChunkMod {

    public static final Logger LOGGER = LoggerFactory.getLogger("HariChunk");

    public HariChunkMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        if (Boolean.getBoolean("com.hari.harichunk.runCompressionBenchmark")) {
            LOGGER.info("Benchmarking chunk stream speed");
            LOGGER.info("Warming up");
            for (int i = 0; i < 3; i++) {
                runBenchmark("GZIP", RegionFileVersion.VERSION_GZIP, true);
                runBenchmark("DEFLATE", RegionFileVersion.VERSION_DEFLATE, true);
                runBenchmark("UNCOMPRESSED", RegionFileVersion.VERSION_NONE, true);
            }
            runBenchmark("GZIP", RegionFileVersion.VERSION_GZIP, false);
            runBenchmark("DEFLATE", RegionFileVersion.VERSION_DEFLATE, false);
            runBenchmark("UNCOMPRESSED", RegionFileVersion.VERSION_NONE, false);
        }
        if (Boolean.getBoolean("com.hari.harichunk.runConsistencyTest")) {
            consistencyTest();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("harichunk")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("status").executes(context -> {
                sendStatus(context.getSource());
                return 1;
            }))
            .executes(context -> {
                context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "HariChunk commands: /harichunk status, /harichunk gpu, /harichunk data"
                ), false);
                return 1;
            }));
    }

    private static String safeDagStats() {
        return invokeStaticString(
                "org.admany.quantifiedadmanydagscheduler.AdmanyDagScheduler",
                "getStatsString"
        );
    }

    private static void sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("HariChunk is loaded."), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "Modules: quantified=" + loaded("quantified")
                        + " bridge=" + loaded("harichunk_x_quantified_api")
                        + " dag=" + loaded("quantified_admany_dag_scheduler")
                        + " vkAccel=" + loaded("quantified_harichunk_opts_vk_gpu_accel")
                        + " brsNoise=" + loaded("harichunk_brs_gpu_noise")
        ), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "QAPI: class=" + classVisible("org.admany.quantified.api.QuantifiedAPI")
                        + " register=" + tryRegisterQapi()
        ), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "DAG: " + safeDagStats()
        ), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
            "GPU backend: " + invokeStaticString("com.hari.harichunk.opts.gpu_noise.common.GpuNoiseBackend", "getStatusString")
        ), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
            "VK accel: " + invokeStaticString("org.admany.vkgpuaccel.VkGpuAccel", "debugString")
        ), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
            "BRS GPU noise: " + invokeStaticString("org.admany.brsgpunoise.BrsGpuNoise", "debugString")
        ), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
            "DFC GPU: " + invokeStaticString("com.hari.harichunk.opts.dfc.common.gen.GpuDensityFunction", "debugString")
        ), false);
    }

    private static boolean loaded(String modId) {
        try {
            return ModList.get().isLoaded(modId);
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static boolean classVisible(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static String tryRegisterQapi() {
        try {
            Class<?> api = Class.forName("org.admany.quantified.api.QuantifiedAPI");
            Method method = api.getMethod("register", String.class, String.class, String.class);
            return String.valueOf(method.invoke(null, "harichunk", "HariChunk", ownVersion()));
        } catch (Throwable throwable) {
            return "failed(" + throwable.getClass().getSimpleName() + ")";
        }
    }

    private static String ownVersion() {
        try {
            return ModList.get()
                    .getModContainerById("harichunk")
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("unknown");
        } catch (Throwable throwable) {
            return "unknown";
        }
    }
    private static String invokeStaticString(String className, String methodName) {
        try {
            Class<?> schedulerClass = Class.forName(className);
            Method method = schedulerClass.getMethod(methodName);
            return String.valueOf(method.invoke(null));
        } catch (Throwable throwable) {
            return "unavailable (" + throwable.getClass().getSimpleName() + ")";
        }
    }

    private void runBenchmark(String name, RegionFileVersion version, boolean suppressLog) {
        try {
            final DecimalFormat decimalFormat = new DecimalFormat("0.###");
            if (!suppressLog) LOGGER.info("Generating 128MB random data");
            final byte[] bytes = new byte[128 * 1024 * 1024];
            new Random().nextBytes(bytes);
            if (!suppressLog) LOGGER.info("Starting benchmark for {}", name);
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            {
                final OutputStream wrappedOutputStream = version.wrap(outputStream);
                long startTime = System.nanoTime();
                wrappedOutputStream.write(bytes);
                wrappedOutputStream.close();
                long endTime = System.nanoTime();
                if (!suppressLog) LOGGER.info("{} write speed: {} MB/s ({} MB/s compressed)", name, decimalFormat.format((bytes.length / 1024.0 / 1024.0) / ((endTime - startTime) / 1_000_000_000.0)), decimalFormat.format((outputStream.size() / 1024.0 / 1024.0) / ((endTime - startTime) / 1_000_000_000.0)));
                if (!suppressLog) LOGGER.info("{} compression ratio: {} %", name, decimalFormat.format(outputStream.size() / (double) bytes.length * 100.0));
            }
            {
                final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                final InputStream wrappedInputStream = version.wrap(inputStream);
                long startTime = System.nanoTime();
                final byte[] readAllBytes = wrappedInputStream.readAllBytes();
                wrappedInputStream.close();
                long endTime = System.nanoTime();
                if (!suppressLog) LOGGER.info("{} read speed: {} MB/s ({} MB/s compressed)", name, decimalFormat.format((readAllBytes.length / 1024.0 / 1024.0) / ((endTime - startTime) / 1_000_000_000.0)), decimalFormat.format((outputStream.size() / 1024.0 / 1024.0) / ((endTime - startTime) / 1_000_000_000.0)));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void consistencyTest() {
        int taskSize = 512;
        AtomicIntegerArray array = new AtomicIntegerArray(taskSize);
        final List<CompletableFuture<Integer>> futures = IntStream.range(0, taskSize)
                .mapToObj(value -> CompletableFuture.supplyAsync(() -> {
                    final WorldgenRandom chunkRandom = new WorldgenRandom(new SingleThreadedRandomSource(System.nanoTime()));
                    chunkRandom.consumeCount(4096);
                    final int i = chunkRandom.nextInt();
                    array.set(value, i);
                    return i;
                }))
                .toList();
        final List<Integer> join = Combinators.collect(futures, Collectors.toList()).toCompletableFuture().join();
        for (int i = 0; i < taskSize; i++) {
            if (array.get(i) != join.get(i))
                throw new IllegalArgumentException("Mismatch at index " + i);
        }
    }
}
