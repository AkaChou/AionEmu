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
	 * Spring 绑定走 setter 写静态字段，绑定结束由 {@code @PostConstruct} 重算派生值；
	 * 该测试固化「setter 只写入、不重算」的约定，避免逐字段重算。
	 * Spring binding writes static fields through setters and recomputes once after binding;
	 * this locks the "setter writes only" contract so recomputation stays a single step.
	 */
	@Test
	void bindingSettersWriteFieldsWithoutRecomputingPoolSize() {
		ThreadConfig.BASE_THREAD_POOL_SIZE = 1;
		ThreadConfig.EXTRA_THREAD_PER_CORE = 4;
		ThreadConfig.load();
		int derived = ThreadConfig.THREAD_POOL_SIZE;

		ThreadConfig config = new ThreadConfig();
		config.setBasepoolsize(9);

		assertEquals(9, ThreadConfig.BASE_THREAD_POOL_SIZE);
		assertEquals(derived, ThreadConfig.THREAD_POOL_SIZE,
			"setter must not recompute the derived pool size");

		config.recomputeAfterBinding();

		int processors = Runtime.getRuntime().availableProcessors();
		assertEquals((9 + 4) * processors, ThreadConfig.THREAD_POOL_SIZE,
			"binding recompute must use the bound base value");
	}
}
