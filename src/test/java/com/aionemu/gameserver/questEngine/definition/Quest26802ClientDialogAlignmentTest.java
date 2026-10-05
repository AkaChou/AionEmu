package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenProgress;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.world.WorldPosition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 26802（16802 的魔族孪生）的真端 DD 行（DD_AREA_HUNT_GRID）合同。
 * <p>
 * 2026-10-05 重锚：26802 已划入 DataDriven 车道（retention basis=DD_AREA_HUNT_GRID），原「生产视图」
 * IR 节点断言随 P7 步 f 退场。现断言面 = DD 运行时公共面：进区域系统发放（acquireZoneInterests，
 * 无 NPC 对话接取面）、击杀网格（killInterests：段 1 = 8 名 Leibo 图书管理员 ×30 落组 1，
 * 段 2 = 6 只 BI Leibo 首领 ×2 落组 2）、组计数推进（真端 {@code counter<target} 守卫；全部组槽达标
 * 收口进 REWARD）；行 5 交付 NPC（reward_npc_name = IDEternity_Q_Feregran_E 806149）的报告面现只对
 * Talk 接取行注册（对象 #2 面），不属本类断言范围。
 * <p>
 * Re-anchored 2026-10-05: 26802 is a DataDriven row (retail area-hunt grid). The assertions read the
 * DD runtime faces: the EnterArea acquire category, the two kill-grid group counters (30 librarians +
 * 2 sub-bosses) and the retail counter guard with all-groups closure into REWARD.
 */
class Quest26802ClientDialogAlignmentTest {
	private static final int QUEST_ID = 26802;
	private static final int STAGE_ONE_KILLS = 30;
	private static final int STAGE_TWO_KILLS = 2;
	/** 客户端段 1 清单（Leibo 图书管理员 8 变体）。 / Client stage-1 list (8 Leibo librarians). */
	private static final Set<Integer> LIBRARIANS = Set.of(
		220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330);
	/** 客户端段 2 清单（BI Leibo 首领 6 变体）。 / Client stage-2 list (6 BI Leibo sub-bosses). */
	private static final Set<Integer> SUB_BOSSES = Set.of(
		857450, 857452, 857454, 857456, 857458, 857459);

	@Test
	void areaGrantStartsTheSequentialChainWithoutNpcRoutes() throws Exception {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		assertTrue(runtime.owns(QUEST_ID), "26802 必须由 DD 运行时拥有");
		assertTrue(runtime.routes(QUEST_ID), "26802 必须由 DD 运行时路由");

		// 接取类别 = 真端表的 EnterArea 行；行内无接取别名（P7 F5 口径：缺席别名 = 死边镜像、
		// 不冻结），且不得有 NPC 对话接取面（旧 XML 的 LevelUp/ZoneMissionEnd/QUEST_ACCEPT_SIMPLE
		// 接取路由随退役入 git 历史）。
		// The row is a retail EnterArea row; it carries no acquire alias (the absent-alias dead-edge
		// mirror) and no NPC talk-acquire face may survive.
		DataDrivenQuestTable table = DataDrivenQuestTable.load(Quest26802ClientDialogAlignmentTest.class
			.getResourceAsStream(DataDrivenNativeRuntime.TABLE_RESOURCE));
		assertEquals("enterarea", table.find(QUEST_ID).orElseThrow().acquireKind(),
			"26802 的接取类别 = 真端 EnterArea 行");
		assertTrue(runtime.acquireTalkInterests().values().stream()
				.noneMatch(questIds -> questIds.contains(QUEST_ID)),
			"进区发放行不得保留 NPC 对话接取面");

		// 击杀网格两组互斥：图书管理员落组 1，首领落组 2（真端 value0 两段计数）。
		for (int npcId : LIBRARIANS) {
			assertTrue(hasKillHit(runtime, npcId, 1), "图书管理员 " + npcId + " 必须落段 1 组槽");
		}
		for (int npcId : SUB_BOSSES) {
			assertTrue(hasKillHit(runtime, npcId, 2), "首领 " + npcId + " 必须落段 2 组槽");
		}

		// 行 5 交付 NPC = 真端 reward_npc_name（IDEternity_Q_Feregran_E = 806149）。其报告/领奖面在
		// 生产 DD 对话平面现只对 Talk 接取行注册（对象 #2 面），不属本类断言范围——此处只锁行数据事实。
		// The row-5 delivery NPC is the retail reward_npc_name (Feregran = 806149); its report face is
		// registered for Talk-acquired rows only and is out of this class's scope.
		assertEquals("IDEternity_Q_Feregran_E", table.find(QUEST_ID).orElseThrow().rewardNpc(),
			"行 5 交付 NPC = 真端 reward_npc_name");
	}

	@Test
	void killEdgesAdvanceOnlyTheFirstUnfinishedStage() {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		Player player = NativeTalkFixture.player();
		place(player, 0f, 0f, 0f);
		QuestState state = NativeTalkFixture.start(player, QUEST_ID);

		// 段 1：图书管理员逐杀推进组 1（30 杀）；组 2 计数独立，顺序填段时纹丝不动。
		for (int kill = 1; kill <= STAGE_ONE_KILLS; kill++) {
			assertTrue(kill(runtime, player, 220306), "段 1 击杀 " + kill + " 必须计数");
			assertEquals(kill, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1),
				"段 1 计数 " + kill);
			assertEquals(0, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 2),
				"段 2 计数不得被段 1 击杀带动");
			assertEquals(QuestStatus.START, state.getStatus(), "两段未齐不得进领奖");
		}
		// 满组超杀零写（真端 counter<target 守卫）。
		assertFalse(kill(runtime, player, 220309), "已满组槽的超杀必须零写");
		assertEquals(STAGE_ONE_KILLS, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1),
			"超杀不得污染组 1 计数");

		// 段 2：首领推进组 2（2 杀）；首个首领不翻态，末杀全部组槽达标 ⇒ 收口进 REWARD（组槽清零）。
		assertTrue(kill(runtime, player, 857450), "段 2 首个首领必须计数");
		assertEquals(1, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 2));
		assertEquals(QuestStatus.START, state.getStatus(), "段 2 未满不得进领奖");
		assertTrue(kill(runtime, player, 857459), "段 2 末杀收口");
		assertEquals(QuestStatus.REWARD, state.getStatus(), "全部组槽达标 ⇒ 收口进领奖");
		assertEquals(0, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 2), "收口清槽");

		// 领奖态：击杀零响应。
		assertFalse(kill(runtime, player, 220306), "领奖态击杀零响应");
	}

	private static boolean kill(DataDrivenNativeRuntime runtime, Player player, int npcId) {
		Npc npc = NativeTalkFixture.npc(npcId);
		place(npc, 0f, 0f, 0f);
		return runtime.onKill(player, npc);
	}

	/** 反射放置坐标（距离门测试面；生产经 WorldPosition 正常初始化）。 / Reflective placement for the distance gate. */
	private static void place(VisibleObject object, float x, float y, float z) {
		WorldPosition position = new WorldPosition(110010000);
		position.setXYZH(x, y, z, (byte) 0);
		object.setPosition(position);
	}

	private static boolean hasKillHit(DataDrivenNativeRuntime runtime, int npcId, int group) {
		List<DataDrivenNativeRuntime.StepHit> hits = runtime.killInterests().get(npcId);
		return hits != null && hits.stream().anyMatch(hit -> hit.questId() == QUEST_ID
			&& hit.stepIndex() == 0 && hit.group() == group);
	}
}
