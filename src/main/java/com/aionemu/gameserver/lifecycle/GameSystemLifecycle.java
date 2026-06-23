package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.utils.AEInfos;
import com.aionemu.gameserver.utils.AEVersions;
import com.aionemu.gameserver.utils.Util;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameSystemLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GameSystemLifecycle.class);
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
    private static final List<String> BANNER_LINES = List.of(
        "Power by Encom / Aion 5.8 Community Project",
        "══════════════════════════════════════════════════════════",
        " █████  ██  ██████  ███    ██ ███████ ███    ███ ██    ██ ███████     █████",
        "██   ██ ██ ██    ██ ████   ██ ██      ████  ████ ██    ██ ██         ██   ██",
        "███████ ██ ██    ██ ██ ██  ██ █████   ██ ████ ██ ██    ██ ███████     █████",
        "██   ██ ██ ██    ██ ██  ██ ██ ██      ██  ██  ██ ██    ██      ██    ██   ██",
        "██   ██ ██  ██████  ██   ████ ███████ ██      ██  ██████  ███████ ██  █████",
        "══════════════════════════════════════════════════════════"
    );

    private final List<Runnable> initializers;
    private final List<String> bannerLines;
    private final LongSupplier currentTimeMillis;
    private final LongSupplier totalMemoryMegabytes;
    private final LongSupplier freeMemoryMegabytes;
    private final LongSupplier maxMemoryMegabytes;
    private final Consumer<String> messageLogger;
    private final MemoryLogger memoryLogger;
    private final LongConsumer startupTimeLogger;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private long startupTimeSeconds = -1;
    private Throwable lastFailure;

    public GameSystemLifecycle() {
        this(
            List.of(
                () -> Util.printSection(" *** System *** "),
                AEVersions::printFullVersionInfo,
                AEInfos::printAllInfos,
                () -> Util.printSection("GameServer")
            ),
            BANNER_LINES,
            System::currentTimeMillis,
            () -> Runtime.getRuntime().totalMemory() / BYTES_PER_MEGABYTE,
            () -> Runtime.getRuntime().freeMemory() / BYTES_PER_MEGABYTE,
            () -> Runtime.getRuntime().maxMemory() / BYTES_PER_MEGABYTE,
            log::info,
            (total, free, used, max) -> log.info(
                "Memory Status After GC: Allocated={} MB, Free={} MB, Used={} MB, Max={} MB",
                total,
                free,
                used,
                max
            ),
            seconds -> log.info("Server startup completed in {} Seconds", seconds)
        );
    }

    GameSystemLifecycle(
        List<Runnable> initializers,
        List<String> bannerLines,
        LongSupplier currentTimeMillis,
        LongSupplier totalMemoryMegabytes,
        LongSupplier freeMemoryMegabytes,
        LongSupplier maxMemoryMegabytes,
        Consumer<String> messageLogger,
        MemoryLogger memoryLogger,
        LongConsumer startupTimeLogger
    ) {
        this.initializers = List.copyOf(initializers);
        this.bannerLines = List.copyOf(bannerLines);
        this.currentTimeMillis = currentTimeMillis;
        this.totalMemoryMegabytes = totalMemoryMegabytes;
        this.freeMemoryMegabytes = freeMemoryMegabytes;
        this.maxMemoryMegabytes = maxMemoryMegabytes;
        this.messageLogger = messageLogger;
        this.memoryLogger = memoryLogger;
        this.startupTimeLogger = startupTimeLogger;
    }

    public synchronized long start(long serverStartTimeMillis) {
        if (loaded) {
            return startupTimeSeconds;
        }

        long start = currentTimeMillis.getAsLong();
        try {
            initializers.forEach(Runnable::run);
            bannerLines.forEach(messageLogger);
            long totalMemory = totalMemoryMegabytes.getAsLong();
            long freeMemory = freeMemoryMegabytes.getAsLong();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = maxMemoryMegabytes.getAsLong();
            memoryLogger.log(totalMemory, freeMemory, usedMemory, maxMemory);
            startupTimeSeconds = (currentTimeMillis.getAsLong() - serverStartTimeMillis) / 1000;
            startupTimeLogger.accept(startupTimeSeconds);
            loaded = true;
            lastFailure = null;
            return startupTimeSeconds;
        } catch (RuntimeException | Error e) {
            loaded = false;
            startupTimeSeconds = -1;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = currentTimeMillis.getAsLong() - start;
        }
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    public synchronized long getStartupTimeSeconds() {
        return startupTimeSeconds;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }

    @FunctionalInterface
    interface MemoryLogger {
        void log(long totalMemory, long freeMemory, long usedMemory, long maxMemory);
    }
}
