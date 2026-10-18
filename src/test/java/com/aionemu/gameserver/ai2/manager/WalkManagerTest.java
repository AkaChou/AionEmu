package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.lifecycle.TestServiceProviders;
import com.aionemu.gameserver.utils.ThreadPoolManager;

class WalkManagerTest {

	private static ThreadPoolManager threadPoolManager;

	@BeforeAll
	static void installThreadPoolProvider() {
		// WalkManager 的静态初始化会注册 Z 检查任务，必须先装好线程池门面。
		// WalkManager's static initializer registers the Z-check task, so the thread pool facade must exist first.
		threadPoolManager = new ThreadPoolManager();
		TestServiceProviders.install(ThreadPoolManager.class, threadPoolManager);
	}

	@AfterAll
	static void clearThreadPoolProvider() {
		TestServiceProviders.clear(ThreadPoolManager.class);
		threadPoolManager.shutdown();
	}

	@Test
	void randomWalkLeashIgnoresSpawnHeightDifference() {
		assertFalse(WalkManager.isOutsideRandomWalkRange(1532.7224f, 1301.1207f,
			1532.7224f, 1301.1207f, 6));
		assertTrue(WalkManager.isOutsideRandomWalkRange(1539, 1301.1207f,
			1532.7224f, 1301.1207f, 6));
	}

	@Test
	void onlyUngroupedWalkersResumeIndividually() {
		assertTrue(WalkManager.shouldResumeIndividually(false, 2),
			"无编队行走者保持个体续走（停在路线点 1m 内时推进下一步）");
		assertFalse(WalkManager.shouldResumeIndividually(false, 0),
			"无当前路线点时走最近点分支");
		assertFalse(WalkManager.shouldResumeIndividually(true, 2),
			"编队成员一律按编队当前步恢复：队长不得单独抢跑下一步"
				+ "（2026-10-06 实机 205294 对话后提前往前走、小动物没跟上）");
	}
}
