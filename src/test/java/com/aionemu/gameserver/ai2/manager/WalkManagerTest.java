package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WalkManagerTest {

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
