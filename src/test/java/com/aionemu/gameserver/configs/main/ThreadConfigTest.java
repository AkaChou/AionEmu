package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@link ThreadConfig} 的派生状态与 Spring 绑定契约。
 * Derived-state and Spring-binding contract for {@link ThreadConfig}.
 */
class ThreadConfigTest {

	private static final int SAVED_BASE = ThreadConfig.BASE_THREAD_POOL_SIZE;
	private static final int SAVED_EXTRA = ThreadConfig.EXTRA_THREAD_PER_CORE;
	private static final long SAVED_RUNTIME = ThreadConfig.MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING;
	private static final boolean SAVED_PRIORITIES = ThreadConfig.USE_PRIORITIES;
	private static final int SAVED_POOL = ThreadConfig.THREAD_POOL_SIZE;

	@AfterEach
	void restore() {
		ThreadConfig.BASE_THREAD_POOL_SIZE = SAVED_BASE;
		ThreadConfig.EXTRA_THREAD_PER_CORE = SAVED_EXTRA;
		ThreadConfig.MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING = SAVED_RUNTIME;
		ThreadConfig.USE_PRIORITIES = SAVED_PRIORITIES;
		ThreadConfig.THREAD_POOL_SIZE = SAVED_POOL;
	}

	/**
	 * 池大小必须是「基础值 + 每核心额外值」乘以当前 CPU 核心数的派生结果。
	 * The pool size must derive from (base + per-core extra) times the current processor count.
	 */
	@Test
	void loadDerivesPoolSizeFromCurrentProperties() {
		ThreadConfig.BASE_THREAD_POOL_SIZE = 3;
		ThreadConfig.EXTRA_THREAD_PER_CORE = 2;

		ThreadConfig.load();

		int processors = Runtime.getRuntime().availableProcessors();
		assertEquals((3 + 2) * processors, ThreadConfig.THREAD_POOL_SIZE);
	}

	/**
	 * setter 写入字段后必须立即把派生池大小收敛到新值，避免 Bean 与静态字段短暂不一致。
	 * A setter must converge the derived pool size immediately, so the bean and the static field
	 * never disagree.
	 */
	@Test
	void bindingSettersRecomputeTheDerivedPoolSize() {
		ThreadConfig config = new ThreadConfig();
		config.setBasepoolsize(9);
		config.setThreadpercore(4);

		int processors = Runtime.getRuntime().availableProcessors();
		assertEquals((9 + 4) * processors, ThreadConfig.THREAD_POOL_SIZE,
			"setters must keep the derived pool size in sync with the bound values");
		assertEquals(ThreadConfig.THREAD_POOL_SIZE, config.getThreadPoolSize());
	}

	/**
	 * {@code @PostConstruct} 兜底重算保证没有任何 setter 被调用时派生值依然正确。
	 * The {@code @PostConstruct} safety-net recomputation keeps the derived value correct even when
	 * no setter ran.
	 */
	@Test
	void postConstructRecomputesTheDerivedPoolSize() {
		ThreadConfig.BASE_THREAD_POOL_SIZE = 2;
		ThreadConfig.EXTRA_THREAD_PER_CORE = 5;
		ThreadConfig.THREAD_POOL_SIZE = 0;

		new ThreadConfig().recomputeAfterBinding();

		int processors = Runtime.getRuntime().availableProcessors();
		assertEquals((2 + 5) * processors, ThreadConfig.THREAD_POOL_SIZE);
	}
}
