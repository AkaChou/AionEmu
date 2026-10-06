package com.aionemu.gameserver.controllers.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 行走贴地短段下发规则的聚焦测试。
 * Focused tests for the walker ground-following stream rules.
 */
class NpcMoveControllerWalkStreamTest {

	@Test
	void streamOnlyForGroundFollowingWalkers() {
		assertTrue(NpcMoveController.shouldStreamWalkGround(true, false, 5f, 1.2f),
				"行走态 + 未到点 + 剩余距离大于前视：应补发贴地短段");
		assertFalse(NpcMoveController.shouldStreamWalkGround(false, false, 5f, 1.2f),
				"非行走子状态不补发（随机行走/追击/返家保持原行为）");
		assertFalse(NpcMoveController.shouldStreamWalkGround(true, true, 5f, 1.2f),
				"已到点时不补发，交给正常的到点广播");
		assertFalse(NpcMoveController.shouldStreamWalkGround(true, false, 1.2f, 1.2f),
				"剩余距离不足前视距离时不补发（收尾段走正常到点广播）");
	}

	@Test
	void streamLookaheadFollowsPerTickTravel() {
		assertEquals(0.26f, NpcMoveController.walkStreamLookahead(0.2f), 1e-4f,
				"前视 = 每 tick 行程 × 1.3（略大于一个下发周期的行程，客户端不会先到点停下）");
		assertEquals(0.15f, NpcMoveController.walkStreamLookahead(0.05f), 1e-4f, "极慢行程取最小前视下限");
	}

	@Test
	void streamTargetStaysWithinLookahead() {
		float[] target = NpcMoveController.walkGroundStreamTarget(0f, 0f, 10f, 5f, 0f, 11f, 1.2f);
		assertEquals(1.2f, target[0], 1e-4f, "前视点应沿段方向前进前视距离");
		assertEquals(0f, target[1], 1e-4f, "侧向不应偏移");
		assertEquals(10.24f, target[2], 1e-4f, "Z 线性回退值供调用方贴地采样");
		assertNull(NpcMoveController.walkGroundStreamTarget(0f, 0f, 0f, 0.5f, 0f, 0f, 1.0f),
				"剩余距离不足前视距离时返回 null");
	}
}
