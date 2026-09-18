package com.aionemu.gameserver.configs.main;

import jakarta.annotation.PostConstruct;
import com.aionemu.commons.configuration.Property;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线程池与运行时告警相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Thread pool and runtime warning configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver.thread")
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
		final int processors = Runtime.getRuntime().availableProcessors();
		THREAD_POOL_SIZE = (BASE_THREAD_POOL_SIZE + extraThreadPerCore) * processors;
	}

	/**
	 * Spring 绑定完成后重算线程池大小；绑定期间每个 setter 都会写入静态字段，
	 * 因此在 {@code @PostConstruct} 统一重算一次即可，避免逐字段重算。
	 *
	 * Recomputes the thread-pool size after Spring binding completes. Each setter already
	 * writes the static field, so one recomputation in {@code @PostConstruct} is enough and
	 * avoids recomputing per field.
	 */
	@PostConstruct
	void recomputeAfterBinding() {
		load();
	}

	public void setBasepoolsize(int basepoolsize) {
		BASE_THREAD_POOL_SIZE = basepoolsize;
	}

	public int getBasepoolsize() {
		return BASE_THREAD_POOL_SIZE;
	}

	public void setThreadpercore(int threadpercore) {
		EXTRA_THREAD_PER_CORE = threadpercore;
	}

	public int getThreadpercore() {
		return EXTRA_THREAD_PER_CORE;
	}

	public void setRuntime(long runtime) {
		MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING = runtime;
	}

	public long getRuntime() {
		return MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING;
	}

	public void setUsepriority(boolean usepriority) {
		USE_PRIORITIES = usepriority;
	}

	public boolean isUsepriority() {
		return USE_PRIORITIES;
	}

	public int getThreadPoolSize() {
		return THREAD_POOL_SIZE;
	}
}
