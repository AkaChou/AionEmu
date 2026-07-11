package com.aionemu.gameserver.lifecycle;

/**
 * 启动进度报告接口：按分组报告加载开始、步骤与结束。
 * Startup progress reporter interface: reports load start, steps and finish per group.
 */
interface StartupProgressReporter {

	/**
	 * 开始报告某分组的加载。
	 * Start reporting load of a group.
	 *
	 * Group name
	 */
	void start(String groupName);

	/**
	 * 某步骤开始。
	 * A step has started.
	 *
	 * 步骤名 / Step name
	 */
	void stepStarted(String stepName);

	/**
	 * 某步骤完成。
	 * A step has finished.
	 *
	 * 步骤名 / Step name
	 */
	void stepFinished(String stepName);

	/**
	 * 结束某分组并报告耗时。
	 * Finish a group and report elapsed time.
	 *
	 * Group name
	 * Elapsed milliseconds
	 */
	void finish(String groupName, long elapsedMillis);

	/**
	 * 报告失败（如清除进度行）。
	 * Report failure (e.g. clear the progress line).
	 */
	void failed();

	/**
	 * 返回空操作实现（不输出任何进度）。
	 * Return a no-op implementation (emits no progress output).
	 *
	 * @return 空操作进度报告器 / No-op progress reporter
	 */
	static StartupProgressReporter noop() {
		return new StartupProgressReporter() {
			@Override
			public void start(String groupName) {
			}

			@Override
			public void stepStarted(String stepName) {
			}

			@Override
			public void stepFinished(String stepName) {
			}

			@Override
			public void finish(String groupName, long elapsedMillis) {
			}

			@Override
			public void failed() {
			}
		};
	}
}
