package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 线程池与运行时告警相关配置。
 * Thread pool and runtime warning related configuration.
 */
public class ThreadConfig {
	/**
	 * 基础线程池大小。
	 * Base thread pool size.
	 */
	@Property(key = "gameserver.thread.basepoolsize", defaultValue = "1")
	public static int BASE_THREAD_POOL_SIZE;
	/**
	 * 每个 CPU 核心额外线程数。
	 * Extra threads per CPU core.
	 */
	@Property(key = "gameserver.thread.threadpercore", defaultValue = "4")
	public static int EXTRA_THREAD_PER_CORE;
	/**
	 * 任务运行超时告警阈值（毫秒）。
	 * Maximum runtime in milliseconds without warning.
	 */
	@Property(key = "gameserver.thread.runtime", defaultValue = "5000")
	public static long MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING;
	/**
	 * 是否为线程设置优先级。
	 * Whether thread priorities are used.
	 */
	@Property(key = "gameserver.thread.usepriority", defaultValue = "false")
	public static boolean USE_PRIORITIES;
	/**
	 * 计算后的最终线程池大小。
	 * Computed final thread pool size.
	 */
	public static int THREAD_POOL_SIZE;

	/**
	 * 根据配置与 CPU 核心数计算线程池大小。
	 * Computes thread pool size from config and available processors.
	 */
	public static void load() {
		final int extraThreadPerCore = EXTRA_THREAD_PER_CORE;
		THREAD_POOL_SIZE = (BASE_THREAD_POOL_SIZE + extraThreadPerCore) * Runtime.getRuntime().availableProcessors();
	}
}
