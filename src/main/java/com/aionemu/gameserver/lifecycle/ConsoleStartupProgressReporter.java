package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.PrintUtils;
import com.aionemu.gameserver.configs.main.GSConfig;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;

/**
 * 控制台启动进度报告器：按分组输出加载进度行。
 * Console startup progress reporter: prints load progress lines per group.
 * <p>
 * 输出统一走 {@code aion.console} logger，与 logback 的其余启动日志保持同一通道与线宽。
 * Output goes through the {@code aion.console} logger so it shares the channel and rule width
 * with the rest of the logback startup output.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
final class ConsoleStartupProgressReporter implements StartupProgressReporter {

	/**
	 * 是否启用进度输出。
	 * Whether progress output is enabled.
	 */
	private final boolean enabled;

	/**
	 * 基于当前控制台与配置创建报告器。
	 * Create a reporter for the current console and configuration.
	 * @return 启动进度报告器 / Startup progress reporter
	 */
	static StartupProgressReporter forCurrentConsole() {
		return new ConsoleStartupProgressReporter(GSConfig.STARTUP_PROGRESS_ENABLE);
	}

	/**
	 * 开始报告某分组的加载。
	 * Start reporting load of a group.
	 * @param groupName 分组名 / Group name
	 */
	@Override
	public void start(String groupName) {
		if (!enabled) {
			return;
		}
		PrintUtils.printSection(I18n.get("console.progress.loading_group", localizedGroupName(groupName)));
	}

	/**
	 * 步骤开始时不输出（保持行干净，直至完成）。
	 * No output on step start (keep the console row clear until completion).
	 * @param stepName 步骤名 / Step name
	 */
	@Override
	public void stepStarted(String stepName) {
		// 步骤完成前保持控制台行空白。 / Keep the console row clear until the step has completed.
	}

	/**
	 * 步骤完成时不再渲染进度行（已移除进度条）。
	 * Step finished; no progress line is rendered (progress bar removed).
	 * @param stepName 步骤名 / Step name
	 */
	@Override
	public void stepFinished(String stepName) {
		// 进度条已移除，步骤完成时不输出。 / Progress bar removed; nothing to output on step finish.
	}

	/**
	 * 结束分组并打印耗时。
	 * Finish a group and print elapsed time.
	 * @param groupName 分组名 / Group name
	 * @param elapsedMillis 耗时毫秒 / Elapsed milliseconds
	 */
	@Override
	public void finish(String groupName, long elapsedMillis) {
		if (!enabled) {
			return;
		}
		PrintUtils.printBannerLine(I18n.get("console.progress.loaded_group", localizedGroupName(groupName), elapsedMillis));
	}

	private String localizedGroupName(String groupName) {
		String key = "console.progress.group." + groupName.replace(' ', '_');
		String localized = I18n.get(key);
		return localized.equals(key) ? groupName : localized;
	}

	/**
	 * 失败时不再清除进度行（已移除进度条）。
	 * Failure; no progress line to clear (progress bar removed).
	 */
	@Override
	public void failed() {
		// 进度条已移除，失败时无需清除。 / Progress bar removed; nothing to clear on failure.
	}
}
