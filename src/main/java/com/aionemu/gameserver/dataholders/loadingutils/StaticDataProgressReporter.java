package com.aionemu.gameserver.dataholders.loadingutils;

/**
 * 静态数据加载进度报告接口，用于在加载各分段时反馈进度。
 * Static-data loading progress reporter used to feedback progress while sections load.
 */
interface StaticDataProgressReporter {

	/**
	 * 开始整体加载。
	 * Starts overall loading.
	 *
	 * total section count
	 */
	void start(int totalSections);

	/**
	 * 某一分段开始加载。
	 * Notifies that a section has started loading.
	 *
	 * @param sectionIndex 当前分段序号 / current section index
	 * total section count
	 * section name
	 * @param totalEntries 分段条目总数 / total entries in the section
	 */
	void sectionStarted(int sectionIndex, int totalSections, String sectionName, int totalEntries);

	/**
	 * 报告某一分段的加载进度。
	 * Reports progress within a section.
	 *
	 * @param sectionIndex 当前分段序号 / current section index
	 * total section count
	 * section name
	 * @param currentEntries 已处理条目数 / entries processed so far
	 * @param totalEntries 分段条目总数 / total entries in the section
	 */
	void sectionProgress(int sectionIndex, int totalSections, String sectionName, int currentEntries, int totalEntries);

	/**
	 * 某一分段加载完成。
	 * Notifies that a section has finished loading.
	 *
	 * @param sectionIndex 当前分段序号 / current section index
	 * total section count
	 * section name
	 * @param totalEntries 分段条目总数 / total entries in the section
	 */
	void sectionFinished(int sectionIndex, int totalSections, String sectionName, int totalEntries);

	/**
	 * 全部加载完成。
	 * Notifies that overall loading has finished.
	 *
	 * total section count
	 * @param elapsedMillis 耗时毫秒数 / elapsed milliseconds
	 */
	void finish(int totalSections, long elapsedMillis);

	/**
	 * 加载失败时调用，用于清理进度输出。
	 * Called when loading fails, to clean up progress output.
	 */
	void failed();

	/**
	 * 返回空操作报告器，忽略全部进度回调。
	 * Returns a no-op reporter that ignores all progress callbacks.
	 *
	 * @return 空操作报告器 / no-op reporter
	 */
	static StaticDataProgressReporter noop() {
		return new StaticDataProgressReporter() {
			@Override
			public void start(int totalSections) {
			}

			@Override
			public void sectionStarted(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
			}

			@Override
			public void sectionProgress(int sectionIndex, int totalSections, String sectionName, int currentEntries, int totalEntries) {
			}

			@Override
			public void sectionFinished(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
			}

			@Override
			public void finish(int totalSections, long elapsedMillis) {
			}

			@Override
			public void failed() {
			}
		};
	}
}
