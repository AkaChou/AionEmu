package com.aionemu.gameserver.controllers.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 行走贴地短段下发规则的聚焦测试。
 * Focused tests for the walker ground-following stream rules.
 */
class NpcMoveControllerWalkStreamTest {

	@Test
	void streamOnlyForGroundFollowingWalkers() {
		assertTrue(NpcMoveController.shouldStreamWalkGround(true, false, 5f),
				"行走态 + 未到点 + 距航点仍有余量：应补发贴地短段");
		assertTrue(NpcMoveController.shouldStreamWalkGround(true, false, 0.9f),
				"收尾段（余量 0.9m）仍补发：此时目标取航点本身，客户端恰在航点停下");
		assertFalse(NpcMoveController.shouldStreamWalkGround(true, false, 0.5f),
				"进入收尾余量（≤0.75m）后停发：再补发会让包目标落进客户端「接近目标即判到达」区间，到点前停-走微抖");
		assertFalse(NpcMoveController.shouldStreamWalkGround(false, false, 5f),
				"非行走子状态不补发（随机行走/追击/返家保持原行为）");
		assertFalse(NpcMoveController.shouldStreamWalkGround(true, true, 5f),
				"已到点时不补发，交给正常的到点广播");
	}

	@Test
	void streamLookaheadFollowsPerTickTravel() {
		assertEquals(1.0f, NpcMoveController.walkStreamLookahead(0.2f), 1e-4f,
				"常规位移下单前视取下限 1m：客户端对近目标「接近即判到达」，前视压短会停-走-停（0.33m 实测）");
		assertEquals(1.95f, NpcMoveController.walkStreamLookahead(1.5f), 1e-4f,
				"单 tick 行程超过 1/1.3m 时前视按 1.3× 行程放大，保证客户端不先到点");
	}

	@Test
	void walkHeadingTurnsGraduallyAtCorners() {
		assertEquals(12f, NpcMoveController.stepHeadingDegrees(0f, 90f, 12f), 1e-4f,
				"拐角朝向逐 tick 过渡：单步不超过最大转角（客户端不会触发原地转身动画）");
		assertEquals(-12f, NpcMoveController.stepHeadingDegrees(0f, -90f, 12f), 1e-4f, "反向取最短转角");
		assertEquals(2f, NpcMoveController.stepHeadingDegrees(350f, 10f, 12f), 1e-4f,
				"跨 ±180° 取最短弧并归一化（350°→10° 只转 +12°）");
		assertEquals(90f, NpcMoveController.stepHeadingDegrees(84f, 90f, 12f), 1e-4f, "差值小于单步时直接到位");
		assertEquals(178f, NpcMoveController.stepHeadingDegrees(-170f, 170f, 12f), 1e-4f,
				"经 ±180° 的最短弧（-170°→170° 走 +20° 方向）");
	}

	@Test
	void streamTargetStaysWithinLookahead() {
		float[] target = NpcMoveController.walkGroundStreamTarget(0f, 0f, 10f, 5f, 0f, 11f, 1.2f);
		assertEquals(1.2f, target[0], 1e-4f, "前视点应沿段方向前进前视距离");
		assertEquals(0f, target[1], 1e-4f, "侧向不应偏移");
		assertEquals(10.24f, target[2], 1e-4f, "Z 线性回退值供调用方贴地采样");

		float[] finalApproach = NpcMoveController.walkGroundStreamTarget(0f, 0f, 0f, 0.5f, 0f, 0f, 1.0f);
		assertEquals(0.5f, finalApproach[0], 1e-4f,
				"剩余距离不足前视距离时收尾段直接取航点本身：客户端恰在航点处停下，避免提前停住后下一段回吸+原地转身");
		assertEquals(0f, finalApproach[1], 1e-4f, "收尾段无侧向偏移");
		assertEquals(0f, finalApproach[2], 1e-4f, "收尾段 Z 取航点值（调用方再贴地）");
	}
}
