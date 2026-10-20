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
 * 系统级收尾生命周期：打印系统信息与内存状态、记录启动耗时，
 * 并在所有启动日志输出完成后打印启动横幅。
 * System-finalization lifecycle: prints system information and memory status, records startup
 * time, and prints the startup banner after all startup logs have been emitted.
 */
@Component
@Slf4j
public class GameSystemLifecycle {

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
		"     _    ___ ___  _   _ _____ __  __ _   _ ",
		"    / \\  |_ _/ _ \\| \\ | | ____|  \\/  | | | |",
		"   / _ \\  | | | | |  \\| |  _| | |\\/| | | | |",
		"  / ___ \\ | | |_| | |\\  | |___| |  | | |_| |",
		" /_/   \\_\\___\\___/|_| \\_|_____|_|  |_|\\___/ ",
		"",
		"                 ____    ___  ",
		"                | ___|  ( _ ) ",
		"                |___ \\  / _ \\ ",
		"                 ___) || (_) |",
		"                |____/  \\___/ ",
		""
	);

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     */
    private boolean loaded;

    /**
     * 本阶段执行耗时毫秒；未启动前为 -1。
     * Stage execution time in milliseconds; {@code -1} before start.
     */
    private long loadTimeMillis = -1;

    /**
     * 服务器整体启动耗时秒数；未成功前为 -1。
     * Overall server startup time in seconds; {@code -1} until success.
     */
    private long startupTimeSeconds = -1;

    /**
     * 最近一次失败。
     * Last failure, if any.
     */
    private Throwable lastFailure;

    /**
     * 启动本阶段：执行系统收尾并返回启动耗时秒数。
     * Start this stage: run system finalization and return startup time in seconds.
     * @param serverStartTimeMillis 服务器启动时间戳（毫秒） / Server start time millis
     * @return 启动耗时秒数；已加载则返回缓存值 / Startup seconds; cached value if already loaded
     */
    public synchronized long start(long serverStartTimeMillis) {
        if (loaded) {
            return startupTimeSeconds;
        }

        long start = System.currentTimeMillis();
        try {
            startupTimeSeconds = printSystemFinalization(serverStartTimeMillis);
            loaded = true;
            lastFailure = null;
            return startupTimeSeconds;
        } catch (RuntimeException | Error e) {
            loaded = false;
            startupTimeSeconds = -1;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    /**
     * 执行系统收尾：打印系统分区、系统信息与内存状态，返回启动耗时秒数。
     * Run system finalization: print the system section, system information, and memory status;
     * return startup seconds.
     *
     * @param serverStartTimeMillis 服务器启动时间戳（毫秒） / Server start time millis
     * @return 启动耗时秒数 / Startup time in seconds
     */
    protected long printSystemFinalization(long serverStartTimeMillis) {
        Util.printSection(I18n.get("console.section.system"));
        AEInfos.printAllInfos();

        long totalMemory = Runtime.getRuntime().totalMemory() / BYTES_PER_MEGABYTE;
        long freeMemory = Runtime.getRuntime().freeMemory() / BYTES_PER_MEGABYTE;
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = Runtime.getRuntime().maxMemory() / BYTES_PER_MEGABYTE;
        log.info(I18n.get("console.memory.status", totalMemory, freeMemory, usedMemory, maxMemory));

        long startupTimeSeconds = (System.currentTimeMillis() - serverStartTimeMillis) / 1000;
        // 收尾耗时由 GameStartupCompletionLifecycle 统一输出，这里降为 DEBUG 以避免重复。 / Completion timing is owned by GameStartupCompletionLifecycle.
        log.debug(I18n.get("console.startup.completed", startupTimeSeconds));
        return startupTimeSeconds;
    }

    /**
     * 输出游戏服务器分区标题与启动横幅。
     * Print the game-server section header and startup banner.
     */
    void printStartupBanner() {
        Util.printSection(I18n.get("console.section.gameserver"));
        bannerLines().forEach(PrintUtils::printBannerLine);
    }

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     * @return 已加载为 {@code true} / {@code true} if loaded
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 本阶段执行耗时毫秒。
     * Stage execution time in milliseconds.
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 服务器整体启动耗时秒数。
     * Overall server startup time in seconds.
     * @return 启动秒数，未成功为 -1 / Startup seconds, or {@code -1} if not successful
     */
    public synchronized long getStartupTimeSeconds() {
        return startupTimeSeconds;
    }

    /**
     * 最近失败。
     * Last failure.
     * @return 最近异常，无则为 null / Last throwable, or {@code null}
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }

	/**
	 * 组装横幅行（艺术字 + 启动成功文案 + 开源声明）。
	 * Build banner lines (art, successful-startup caption, then the open-source notice).
	 *
	 * <p>开源声明无配置开关、固定输出在启动成功横幅之后，便于用户在每次启动时确认本软件免费开源，
	 * 避免从第三方渠道付费购买或被误导；横幅不输出仓库地址；输出经 {@code aion.console}
	 * 写入控制台与 console.log。
	 * The open-source notice has no config switch and always follows the startup caption so users can
	 * confirm on every boot that the software is free and open source, guarding against paid resale or
	 * misleading third-party channels; the banner omits the repository URL and is written through
	 * {@code aion.console} to the console and console.log.</p>
	 *
	 * @return 横幅行列表 / Banner line list
	 */
	static List<String> bannerLines() {
		List<String> lines = new ArrayList<>(BANNER_ART.size() + 4);
		lines.addAll(BANNER_ART);
		lines.add("  " + I18n.get("console.startup.successful"));
		lines.add("");
		lines.add("  " + I18n.get("console.startup.open_source_notice"));
		lines.add("  " + I18n.get("console.startup.open_source_license"));
		return lines;
	}
}
