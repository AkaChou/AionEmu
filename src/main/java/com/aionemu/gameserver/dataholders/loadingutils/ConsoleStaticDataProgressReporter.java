package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.PrintStream;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.utils.ConsoleProgressLineRenderer;

/**
 * 控制台静态数据加载进度报告器，将各分段进度渲染到启动控制台。
 * Console static-data progress reporter that renders section progress on the startup console.
 */
final class ConsoleStaticDataProgressReporter implements StaticDataProgressReporter {

	private static final String SECTION_SEPARATOR = "────────────────────────────────────────────────────────";

	private final PrintStream out;
	private final boolean enabled;
	private final ConsoleProgressLineRenderer progressRenderer;

	/**
	 * 使用指定输出流与启用开关创建报告器。
	 * Creates a reporter with the given output stream and enable flag.
	 *
	 * output stream
	 * whether enabled
	 */
	ConsoleStaticDataProgressReporter(PrintStream out, boolean enabled) {
		this.out = out;
		this.enabled = enabled;
		this.progressRenderer = new ConsoleProgressLineRenderer(out, enabled);
	}

	/**
	 * 为当前控制台创建报告器，启用状态取自启动进度配置。
	 * Creates a reporter for the current console, gated by the startup-progress config.
	 *
	 * @return 控制台进度报告器 / console progress reporter
	 */
	static StaticDataProgressReporter forCurrentConsole() {
		return new ConsoleStaticDataProgressReporter(System.out, GSConfig.STARTUP_PROGRESS_ENABLE);
	}

	/**
	 * 开始加载：打印分段分隔线与加载提示。
	 * Starts loading: prints the section separator and loading banner.
	 *
	 * total section count
	 */
	@Override
	public void start(int totalSections) {
		if (!progressEnabled()) {
			return;
		}
		out.println(SECTION_SEPARATOR);
		out.println(I18n.get("console.static_data.loading"));
		out.flush();
	}

	/**
	 * 分段开始回调；控制台行在有实际进度前保持空白。
	 * Section-start callback; the console row stays clear until there is actual progress to render.
	 *
	 * @param sectionIndex 当前分段序号 / current section index
	 * total section count
	 * section name
	 * @param totalEntries 分段条目总数 / total entries in the section
	 */
	@Override
	public void sectionStarted(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
		// 在有实际进度可渲染前保持控制台行空白。 / Keep the console row clear until there is actual progress to render.
	}

	/**
	 * 更新当前分段的进度行。
	 * Updates the progress line for the current section.
	 *
	 * @param sectionIndex 当前分段序号 / current section index
	 * total section count
	 * section name
	 * @param currentEntries 已处理条目数 / entries processed so far
	 * @param totalEntries 分段条目总数 / total entries in the section
	 */
	@Override
	public void sectionProgress(int sectionIndex, int totalSections, String sectionName, int currentEntries, int totalEntries) {
		if (!progressEnabled()) {
			return;
		}
		progressRenderer.progress(sectionName, currentEntries, totalEntries);
	}

	/**
	 * 标记当前分段完成并渲染完成行。
	 * Marks the current section finished and renders the completion line.
	 *
	 * @param sectionIndex 当前分段序号 / current section index
	 * total section count
	 * section name
	 * @param totalEntries 分段条目总数 / total entries in the section
	 */
	@Override
	public void sectionFinished(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
		if (!progressEnabled()) {
			return;
		}
		progressRenderer.finished(sectionName, totalEntries);
	}

	/**
	 * 全部加载完成：打印耗时摘要。
	 * Finishes loading: prints the elapsed-time summary.
	 *
	 * total section count
	 * @param elapsedMillis 耗时毫秒数 / elapsed milliseconds
	 */
	@Override
	public void finish(int totalSections, long elapsedMillis) {
		if (!enabled) {
			return;
		}
		out.println(I18n.get("console.static_data.loaded", elapsedMillis));
		out.flush();
	}

	/**
	 * 加载失败时清除当前进度行。
	 * Clears the current progress line when loading fails.
	 */
	@Override
	public void failed() {
		if (!progressEnabled()) {
			return;
		}
		progressRenderer.clearLine();
	}

	/**
	 * 判断条目级进度输出是否启用。
	 * Returns whether entry-level progress output is enabled.
	 *
	 * @return 若 enabled 则为 true / true if enabled
	 */
	private boolean progressEnabled() {
		return enabled && GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE;
	}
}
