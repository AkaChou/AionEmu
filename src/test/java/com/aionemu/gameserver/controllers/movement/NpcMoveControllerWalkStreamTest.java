package com.aionemu.gameserver.controllers.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.Geometry;
import com.aionemu.gameserver.geoEngine.scene.Mesh;
import com.aionemu.gameserver.geoEngine.scene.VertexBuffer.Type;

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

	@Test
	void walkerPathModeGatesOnModeAndLegVerdict() {
		assertFalse(NpcMoveController.shouldUseWalkerPath("off", true),
				"off 模式恒不沿 Path（回滚态）：与旧行为逐分支等价");
		assertFalse(NpcMoveController.shouldUseWalkerPath(null, true), "未配置视为 off");
		assertTrue(NpcMoveController.shouldUseWalkerPath("blocked", true),
				"blocked 模式只对 PATH-LoS 失败的段启用沿 Path");
		assertFalse(NpcMoveController.shouldUseWalkerPath("blocked", false),
				"blocked 模式下直线可达段保持直线（零 A*，平地行为逐字节不变）");
		assertTrue(NpcMoveController.shouldUseWalkerPath("always", false),
				"always 模式（实机 A/B 用）全段沿 Path");
		assertTrue(NpcMoveController.shouldUseWalkerPath("BLOCKED", true), "模式值大小写不敏感");
	}

	@Test
	void pathStreamTargetFollowsPolylineArcs() {
		float[][] leg = {{2f, 0f, 10f}, {4f, 0f, 10f}};
		float[] onPolyline = NpcMoveController.walkPathStreamTarget(0f, 0f, 10f, leg, 5f, 0f, 10f, 3f);
		assertEquals(3f, onPolyline[0], 1e-4f,
				"折线取点按弧长计：先走完 2m 节点段，再在节点段上取 1m（不是直线弦上的点）");
		assertEquals(0f, onPolyline[1], 1e-4f);
		assertEquals(10f, onPolyline[2], 1e-4f);

		float[] onWaypointLeg = NpcMoveController.walkPathStreamTarget(0f, 0f, 0f, new float[][] {{1f, 0f, 0f}},
				3f, 0f, 0f, 2f);
		assertEquals(2f, onWaypointLeg[0], 1e-4f, "折线走完后的收尾段按弧长在「末节点 → 航点」段上取点");

		float[] waypointItself = NpcMoveController.walkPathStreamTarget(0f, 0f, 0f, new float[][] {{0.5f, 0f, 0f}},
				1f, 0f, 0f, 2f);
		assertEquals(1f, waypointItself[0], 1e-4f,
				"整条折线（含到航点的一段）不超过前视距离时返回航点本身：与直线版收尾分支同语义");

		float[] straight = NpcMoveController.walkGroundStreamTarget(0f, 0f, 0f, 5f, 0f, 1f, 1.2f);
		float[] straightViaPath = NpcMoveController.walkPathStreamTarget(0f, 0f, 0f, null, 5f, 0f, 1f, 1.2f);
		assertEquals(straight[0], straightViaPath[0], 1e-4f, "Path 为 null 时与直线版等价（对拍 x）");
		assertEquals(straight[2], straightViaPath[2], 1e-4f, "Path 为 null 时与直线版等价（对拍 z）");
	}

	@Test
	void pathStreamRemainingCountsPolylineArcs() {
		float[][] leg = {{2f, 0f, 0f}, {2f, 3f, 0f}};
		assertEquals(7f, NpcMoveController.walkPathRemaining(0f, 0f, 0f, leg, 2f, 5f, 0f), 1e-4f,
				"剩余量 = 当前位置沿折线到航点的总弧长（2 + 3 + 2），供 0.75m 收尾门槛使用");
		assertEquals(5f, NpcMoveController.walkPathRemaining(0f, 0f, 0f, null, 3f, 4f, 0f), 1e-4f,
				"Path 为 null 时退化为到航点的水平距离（与直线版同语义）");
	}

	@Test
	void formationLeaderWalksPathWhileOffsetFollowersStayStraight() {
		assertFalse(NpcMoveController.isFormationFollower(false, false, 0f, 0f), "非编队：不是跟随者");
		assertFalse(NpcMoveController.isFormationFollower(true, true, 0f, 0f),
				"队长（shift 0,0）站位与航点重合：按独行语义处理，可沿 Path");
		assertFalse(NpcMoveController.isFormationFollower(true, true, 0.005f, -0.005f),
				"浮点积差级别的近似零偏移：仍视为队长");
		assertTrue(NpcMoveController.isFormationFollower(true, true, 0f, -8f),
				"offset −8m 成员：航点被偏移到成员站位，保持直线成员段（AIM-009/010）");
		assertTrue(NpcMoveController.isFormationFollower(true, true, -1f, 0f), "任意非零偏移即跟随者");
		assertTrue(NpcMoveController.isFormationFollower(true, false, 0f, 0f),
				"编队内缺 shift（异常态）：保守按跟随者排除");
		assertTrue(NpcMoveController.walkerPathEligible(true, false, false, false), "独行/队长 + 开关开：有资格");
		assertFalse(NpcMoveController.walkerPathEligible(true, true, false, false), "跟随者：无资格");
		assertFalse(NpcMoveController.walkerPathEligible(false, false, false, false), "PATH 总开关关：无资格");
		assertFalse(NpcMoveController.walkerPathEligible(true, false, true, false), "飞行：无资格");
		assertFalse(NpcMoveController.walkerPathEligible(true, false, false, true), "空间寻路（游泳/飞行三维）：无资格");
	}

	@Test
	void sameLegTargetDedupesDuplicateRefreshes() {
		assertTrue(NpcMoveController.sameWalkerLegTarget(1679.35f, 1478.03f, 121.363f,
				1679.35f, 1478.03f, 121.37f), "同航点、Z 因重解地面抖动 1cm：视为同一段（复用判定与在途请求）");
		assertFalse(NpcMoveController.sameWalkerLegTarget(1679.35f, 1478.03f, 121.363f,
				1670.65f, 1477.99f, 121.056f), "相邻航点（8m 外）：不是同一段");
		assertFalse(NpcMoveController.sameWalkerLegTarget(1679.35f, 1478.03f, 121.363f,
				Float.NaN, Float.NaN, Float.NaN), "未记录（NaN 键）：不视为重复");
		assertFalse(NpcMoveController.sameWalkerLegTarget(1679.35f, 1478.03f, 121.363f,
				1679.35f, 1478.03f, 125.0f), "同 XY 但 Z 差 3.6m（不同层航点）：不是同一段");
	}

	@Test
	void walkerCollisionLiftCapsAtRetailStepHeight() {
		assertEquals(1.5f, NpcMoveController.walkerLiftOffset(1), 1e-4f, "真端步高抬升单步 1.5m");
		assertEquals(2.0f, NpcMoveController.walkerLiftOffset(2), 1e-4f, "第二次抬升封顶在 2.0m（3.0 被截）");
		assertEquals(2.0f, NpcMoveController.walkerLiftOffset(9), 1e-4f, "上限对整个循环生效（真端 NPC 自带上限 2.0）");
	}

	@Test
	void walkerCollisionStepStopsBeforeWallsAndLeavesOpenGroundAlone() {
		assertNull(NpcMoveController.resolveWalkerCollisionStep(null, 1, 0f, 0f, 0f, 4f, 0f, 0f),
				"无地图时不干预（交给逐 tick 贴地）");
		assertNull(NpcMoveController.resolveWalkerCollisionStep(new GeoMap("1", 256), 1, 0f, 0f, 0f, 4f, 0f, 0f),
				"空旷地面水平可通行：返回 null，由逐 tick 贴地接管");
		GeoMap wall = mapWithQuad(new float[] {2, -1, -1, 2, 1, -1, 2, 1, 2, 2, -1, 2});
		float[] resolved = NpcMoveController.resolveWalkerCollisionStep(wall, 1, 0f, 0f, 0f, 4f, 0f, 0f);
		assertNotNull(resolved, "高墙（抬升 9 次仍不通）被挡：必须返回解算点，不能穿墙");
		assertTrue(resolved[0] < 2f, "解算点截断在墙前（x < 2），水平不穿透");
	}

	private static GeoMap mapWithQuad(float[] positions) {
		Mesh mesh = new Mesh();
		mesh.setBuffer(Type.Position, 3, positions);
		mesh.setBuffer(Type.Index, 3, new int[] {0, 1, 2, 0, 2, 3});
		mesh.setCollisionFlags((short) (CollisionIntention.PHYSICAL.getId() << 8));

		Geometry geometry = new Geometry("obstacle", mesh);
		geometry.updateModelBound();
		GeoMap map = new GeoMap("1", 256);
		map.attachChild(geometry);
		map.updateModelBound();
		return map;
	}
}
