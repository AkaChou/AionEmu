package com.aionemu.gameserver.lifecycle;

import java.io.PrintStream;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.utils.ConsoleProgressLineRenderer;

/**
 * 控制台启动进度报告器：按分组输出加载进度行。
 * Console startup progress reporter: prints load progress lines per group.
 */
final class ConsoleStartupProgressReporter implements StartupProgressReporter {

	/**
	 * 分组分隔线。
	 * Section separator line.
	 */
	private static final String SECTION_SEPARATOR = "────────────────────────────────────────────────────────";

	/**
	 * 输出流。
	 * Output stream.
	 */
	private final PrintStream out;

	/**
	 * 是否启用进度输出。
	 * Whether progress output is enabled.
	 */
	private final boolean enabled;

	/**
	 * 控制台进度行渲染器。
	 * Console progress-line renderer.
	 */
	private final ConsoleProgressLineRenderer progressRenderer;

	/**
	 * 构造控制台进度报告器。
	 * Construct a console progress reporter.
	 *
	 * @param out 输出流 / Output stream
	 * @param enabled 是否启用 / Whether enabled
	 */
	ConsoleStartupProgressReporter(PrintStream out, boolean enabled) {
		this.out = out;
		this.enabled = enabled;
		this.progressRenderer = new ConsoleProgressLineRenderer(out, enabled);
	}

	/**
	 * 基于当前控制台与配置创建报告器。
	 * Create a reporter for the current console and configuration.
	 *
	 * @return 启动进度报告器 / Startup progress reporter
	 */
	static StartupProgressReporter forCurrentConsole() {
		return new ConsoleStartupProgressReporter(System.out, GSConfig.STARTUP_PROGRESS_ENABLE);
	}

	/**
	 * 开始报告某分组的加载。
	 * Start reporting load of a group.
	 *
	 * @param groupName 分组名 / Group name
	 */
	@Override
	public void start(String groupName) {
		if (!enabled) {
			return;
		}
		out.println(SECTION_SEPARATOR);
		out.println(I18n.get("console.progress.loading_group", localizedGroupName(groupName)));
		out.flush();
	}

	/**
	 * 步骤开始时不输出（保持行干净，直至完成）。
	 * No output on step start (keep the console row clear until completion).
	 *
	 * @param stepName 步骤名 / Step name
	 */
	@Override
	public void stepStarted(String stepName) {
		// 步骤完成前保持控制台行空白。 / Keep the console row clear until the step has completed.
	}

	/**
	 * 步骤完成时渲染进度行。
	 * Render the progress line when a step finishes.
	 *
	 * @param stepName 步骤名 / Step name
	 */
	@Override
	public void stepFinished(String stepName) {
		progressRenderer.finished(stepName, 1);
	}

	/**
	 * 结束分组并打印耗时。
	 * Finish a group and print elapsed time.
	 *
	 * @param groupName 分组名 / Group name
	 * @param elapsedMillis 耗时毫秒 / Elapsed milliseconds
	 */
	@Override
	public void finish(String groupName, long elapsedMillis) {
		if (!enabled) {
			return;
		}
		out.println(I18n.get("console.progress.loaded_group", localizedGroupName(groupName), elapsedMillis));
		out.flush();
	}

	private String localizedGroupName(String groupName) {
		String key = "console.progress.group." + groupName.replace(' ', '_');
		String localized = I18n.get(key);
		return localized.equals(key) ? groupName : localized;
	}

	/**
	 * 失败时清除当前进度行。
	 * Clear the current progress line on failure.
	 */
	@Override
	public void failed() {
		progressRenderer.clearLine();
	}
}
