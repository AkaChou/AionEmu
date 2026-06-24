package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.utils.AEInfos;
import com.aionemu.gameserver.utils.AEVersions;
import com.aionemu.gameserver.utils.Util;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameSystemGateway {

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

    public long start(long serverStartTimeMillis) {
        Util.printSection(" *** System *** ");
        AEVersions.printFullVersionInfo();
        AEInfos.printAllInfos();
        Util.printSection("GameServer");
        BANNER_LINES.forEach(log::info);

        long totalMemory = Runtime.getRuntime().totalMemory() / BYTES_PER_MEGABYTE;
        long freeMemory = Runtime.getRuntime().freeMemory() / BYTES_PER_MEGABYTE;
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = Runtime.getRuntime().maxMemory() / BYTES_PER_MEGABYTE;
        log.info(
            "Memory Status After GC: Allocated={} MB, Free={} MB, Used={} MB, Max={} MB",
            totalMemory,
            freeMemory,
            usedMemory,
            maxMemory
        );

        long startupTimeSeconds = (System.currentTimeMillis() - serverStartTimeMillis) / 1000;
        log.info("Server startup completed in {} Seconds", startupTimeSeconds);
        return startupTimeSeconds;
    }
}
