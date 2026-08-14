package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.utils.PrintUtils;
import com.aionemu.gameserver.utils.Util;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 系统级收尾网关：打印系统信息、横幅与内存，并计算启动耗时。
 * System-finalization gateway: prints system info, banner and memory, and computes startup time.
 */
@Component
@Slf4j
public class GameSystemGateway {

    /**
     * 每兆字节的字节数。
     * Bytes per megabyte.
     */
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    // ponytail: fixed AionEmu banner art; caption via I18n
    // ponytail: 固定的 AionEmu 横幅艺术字；文案经 I18n 提供。
    /**
     * 固定 AionEmu 横幅艺术字行（文案经 I18n）。
     * Fixed AionEmu banner art lines (caption via I18n).
     */
    private static final List<String> BANNER_ART = List.of(
        "",
        "  █████╗   ██╗   ██████╗   ███╗   ██╗  ███████╗  ███╗   ███╗  ██╗   ██╗",
        " ██╔══██╗  ██║  ██╔═══██╗  ████╗  ██║  ██╔════╝  ████╗ ████║  ██║   ██║",
        " ███████║  ██║  ██║   ██║  ██╔██╗ ██║  █████╗    ██╔████╔██║  ██║   ██║",
        " ██╔══██║  ██║  ██║   ██║  ██║╚██╗██║  ██╔══╝    ██║╚██╔╝██║  ██║   ██║",
        " ██║  ██║  ██║  ╚██████╔╝  ██║ ╚████║  ███████╗  ██║ ╚═╝ ██║  ╚██████╔╝",
        " ╚═╝  ╚═╝  ╚═╝   ╚═════╝   ╚═╝  ╚═══╝  ╚══════╝  ╚═╝     ╚═╝   ╚═════╝ ",
        "",
        "                           ███████╗   █████╗ ",
        "                           ██╔════╝  ██╔══██╗",
        "                           ███████╗  ╚█████╔╝",
        "                           ╚════██║  ██╔══██╗",
        "                           ███████║  ╚█████╔╝",
        "                           ╚══════╝   ╚════╝ ",
        ""
    );

    /**
     * 执行系统收尾：打印分区、信息、横幅与内存，返回启动耗时秒数。
     * Run system finalization: print sections, info, banner and memory; return startup seconds.
     *
     * @param serverStartTimeMillis 服务器启动时间戳（毫秒） / Server start time millis
     * @return 启动耗时秒数 / Startup time in seconds
     */
    public long start(long serverStartTimeMillis) {
        Util.printSection(I18n.get("console.section.system"));
        AEInfos.printAllInfos();
        Util.printSection(I18n.get("console.section.gameserver"));
        bannerLines().forEach(PrintUtils::printBannerLine);

        long totalMemory = Runtime.getRuntime().totalMemory() / BYTES_PER_MEGABYTE;
        long freeMemory = Runtime.getRuntime().freeMemory() / BYTES_PER_MEGABYTE;
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = Runtime.getRuntime().maxMemory() / BYTES_PER_MEGABYTE;
        log.info(I18n.get("console.memory.status", totalMemory, freeMemory, usedMemory, maxMemory));

        long startupTimeSeconds = (System.currentTimeMillis() - serverStartTimeMillis) / 1000;
        log.info(I18n.get("console.startup.completed", startupTimeSeconds));
        return startupTimeSeconds;
    }

    /**
     * 组装横幅行（艺术字 + 启动成功文案）。
     * Build banner lines (art plus successful-startup caption).
     *
     * @return 横幅行列表 / Banner line list
     */
    private static List<String> bannerLines() {
        List<String> lines = new ArrayList<>(BANNER_ART.size() + 1);
        lines.addAll(BANNER_ART);
        lines.add("  " + I18n.get("console.startup.successful"));
        return lines;
    }
}
