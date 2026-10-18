package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.PrintUtils;
import com.aionemu.gameserver.configs.main.GSConfig;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;

/**
 * 控制台静态数据加载进度报告器，将各分段进度渲染到启动控制台。
 * Console static-data progress reporter that renders section progress on the startup console.
 * <p>
 * 明细行统一走 {@code aion.console} 的 DEBUG 级别；INFO 控制台的静态数据里程碑由
 * {@code DataManager} 的分区标题与加载结果行承担，避免重复输出。
 * Detail lines run at the {@code aion.console} DEBUG level; the INFO static-data milestone is
 * owned by {@code DataManager}'s section header and result line, so nothing is duplicated.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
final class ConsoleStaticDataProgressReporter implements StaticDataProgressReporter {

	private final boolean enabled;

	/**
	 * 为当前控制台创建报告器，启用状态取自启动进度配置。
	 * Creates a reporter for the current console, gated by the startup-progress config.
	 * @return 控制台进度报告器 / console progress reporter
	 */
	static StaticDataProgressReporter forCurrentConsole() {
		return new ConsoleStaticDataProgressReporter(GSConfig.STARTUP_PROGRESS_ENABLE);
	}

	/**
	 * 开始加载：以 DEBUG 输出分段标题，不打扰 INFO 控制台。
	 * Starts loading: prints the section title at DEBUG, keeping the INFO console quiet.
	 * @param totalSections 分段总数 / total section count
	 */
	@Override
	public void start(int totalSections) {
		if (!progressEnabled()) {
			return;
		}
		PrintUtils.printSectionIfVerbose(I18n.get("console.static_data.loading"));
	}

	/**
	 * 分段开始回调；控制台行在有实际进度前保持空白。
	 * Section-start callback; the console row stays clear until there is actual progress to render.
	 * @param sectionIndex 当前分段序号 / current section index
	 * @param totalSections 分段总数 / total section count
	 * @param sectionName 分段名称 / section name
	 * @param totalEntries 分段条目总数 / total entries in the section
	 */
	@Override
	public void sectionStarted(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
		// 在有实际进度可渲染前保持控制台行空白。 / Keep the console row clear until there is actual progress to render.
	}

	/**
	 * 更新当前分段的进度行（进度条已移除，不再输出）。
	 * Updates the progress line for the current section (no-op; progress bar removed).
	 * @param sectionIndex 当前分段序号 / current section index
	 * @param totalSections 分段总数 / total section count
	 * @param sectionName 分段名称 / section name
	 * @param currentEntries 已处理条目数 / entries processed so far
	 * @param totalEntries 分段条目总数 / total entries in the section
	 */
	@Override
	public void sectionProgress(int sectionIndex, int totalSections, String sectionName, int currentEntries, int totalEntries) {
		// 进度条已移除，分段进度不再输出。 / Progress bar removed; section progress no longer rendered.
	}

	/**
	 * 标记当前分段完成（进度条已移除，不再渲染完成行）。
	 * Marks the current section finished (no-op; progress bar removed).
	 * @param sectionIndex 当前分段序号 / current section index
	 * @param totalSections 分段总数 / total section count
	 * @param sectionName 分段名称 / section name
	 * @param totalEntries 分段条目总数 / total entries in the section
	 */
	@Override
	public void sectionFinished(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
		// 进度条已移除，分段完成不再渲染。 / Progress bar removed; section finish no longer rendered.
	}

	/**
	 * 全部加载完成：不再输出耗时（由 {@code DataManager} 的加载耗时行统一承担）。
	 * Finishes loading: no elapsed output (the {@code DataManager} timing line owns it).
	 * @param totalSections 分段总数 / total section count
	 * @param elapsedMillis 耗时毫秒数 / elapsed milliseconds
	 */
	@Override
	public void finish(int totalSections, long elapsedMillis) {
		// 静态数据耗时由 DataManager 的权威结果行输出，这里不再重复。 / Timing is owned by DataManager's authoritative result line.
	}

	/**
	 * 加载失败时不再清除进度行（进度条已移除）。
	 * Clears the current progress line when loading fails (no-op; progress bar removed).
	 */
	@Override
	public void failed() {
		// 进度条已移除，失败时无需清除。 / Progress bar removed; nothing to clear on failure.
	}

	/**
	 * 判断条目级进度输出是否启用。
	 * Returns whether entry-level progress output is enabled.
	 * @return 若 enabled 则为 true / true if enabled
	 */
	private boolean progressEnabled() {
		return enabled && GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE;
	}
}
