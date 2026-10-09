package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenProgress;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.world.WorldPosition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 13944（[연합] 신성의 요새 상급 정찰부대 처치）的原版 DD 行（Talk + Hunt）与三杀交付。
 * Locks quest 13944's retail DD row (Talk + Hunt) and its three-kill hand-in.
 * <p>
 * 已退役（保留清单 owner=RETAIL_TABLE，family=DataDriven）：旧测试直接读生产 quests/13944.xml（已删除，
 * XML 只在 git 历史里）随迁移退场，按计划 §8.9（P3 重锚口径）改锚 DD 运行时公共面：Talk 接取
 * （Ab1_Dian_E）、两变体三杀网格（落组 1）、组槽达标收口进 REWARD、交付 NPC 的奖励窗面。
 * <p>
 * Re-anchored (plan §8.9) to the DD runtime faces: the Talk acquire row (Ab1_Dian_E), the three-kill grid
 * over the two scout variants (group 1), the all-groups closure into REWARD and the reward-window face.
 */
class Quest13944RetailAlignmentTest {
	private static final int QUEST_ID = 13944;
	private static final int DIAN_NPC_ID = 835722;
	private static final int OBJECT_ID = 900_044;
	private static final String OUTSIDE_SCOUT = "Ab1_1011_Outside_Guard_Fi_Dr";
	private static final String INSIDE_SCOUT = "Ab1_1011_Inside_Guard_Fi_Dr";
	private static final int KILLS_REQUIRED = 3;

	@Test
	void retailRowKeepsTheUnionAcquireAndTheThreeKillGrid() throws Exception {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		assertTrue(runtime.owns(QUEST_ID), "13944 必须由 DD 运行时拥有");
		assertTrue(runtime.routes(QUEST_ID), "13944 必须由 DD 运行时路由");
		assertEquals(DIAN_NPC_ID, NativeNpcNameResolver.instance().resolve("Ab1_Dian_E").npcIds().get(0));

		DataDrivenQuestTable table = DataDrivenQuestTable.load(
			Quest13944RetailAlignmentTest.class
				.getResourceAsStream(DataDrivenNativeRuntime.TABLE_RESOURCE));
		DataDrivenQuestTable.Row row = table.find(QUEST_ID).orElseThrow();
		assertEquals("talk", row.acquireKind(), "接取类别 = 原版 Talk 行");
		assertEquals("Ab1_Dian_E", row.acquireParam(), "接取 NPC 名 = 原版 value0_acquire_");
		assertEquals("Ab1_Dian_E", row.rewardNpc(), "交付 NPC 名 = 原版 reward_npc_name");
		assertTrue(runtime.acquireTalkInterests().getOrDefault(DIAN_NPC_ID, List.of()).contains(QUEST_ID),
			"Dian 必须注册 13944 的接取谈话面");

		// 击杀网格：两变体（内外哨兵）同落组 1。
		List<Integer> outside = NativeNpcNameResolver.instance().resolveMonsterIds(OUTSIDE_SCOUT);
		List<Integer> inside = NativeNpcNameResolver.instance().resolveMonsterIds(INSIDE_SCOUT);
		assertTrue(!outside.isEmpty() && !inside.isEmpty(), "两变体名必须可解析");
		for (int mobId : outside) {
			assertTrue(hasKillHit(runtime, mobId, 1), "外哨 " + mobId + " 必须落组 1 组槽");
		}
		for (int mobId : inside) {
			assertTrue(hasKillHit(runtime, mobId, 1), "内哨 " + mobId + " 必须落组 1 组槽");
		}

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(66, metadata.minLevel(), "原版 minlevel_permitted=66");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "原版 pc_light");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 93626245)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 188058110, 1)), () -> rewards.toString());
	}

	@Test
	void acceptGridWalkAndRewardFacesFollowTheRetailRow() {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 66);
		place(player, 0f, 0f, 0f);

		// 未接取：任务行打开原版接取入口页（4762 = 客户端声明的 select_none），带任务上下文。
		// Dian 同时服务 13943 的接取面 ⇒ 必须带客户端任务上下文（requestedOwner）精确定位本行。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, DIAN_NPC_ID, 31, OBJECT_ID, QUEST_ID));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 4762, QUEST_ID);

		// 接取收尾（20000）：建档 START + 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, DIAN_NPC_ID, 20000, OBJECT_ID, QUEST_ID));
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.START, state.getStatus(), "接取收尾必须把任务建到 START");
		NativeTalkFixture.assertCloseDialog(player);

		// 网格推进：两变体共 3 杀；前 2 杀保持 START，第 3 杀组槽达标 ⇒ 收口进 REWARD（清槽）。
		int outsideId = NativeNpcNameResolver.instance().resolveMonsterIds(OUTSIDE_SCOUT).get(0);
		int insideId = NativeNpcNameResolver.instance().resolveMonsterIds(INSIDE_SCOUT).get(0);
		assertTrue(kill(runtime, player, outsideId), "第 1 杀必须计数");
		assertEquals(1, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1));
		assertEquals(QuestStatus.START, state.getStatus(), "未满 3 杀不得进领奖");
		assertTrue(kill(runtime, player, insideId), "第 2 杀必须计数");
		assertEquals(2, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1));
		assertTrue(kill(runtime, player, outsideId), "第 3 杀收口");
		assertEquals(QuestStatus.REWARD, state.getStatus(), "组槽达标 ⇒ 收口进领奖");
		assertEquals(0, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1), "收口清槽");

		// 领奖面：交付 NPC 上任务行弹奖励窗（页 5，带任务上下文；同样带 requestedOwner 定位本行）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, DIAN_NPC_ID, 31, OBJECT_ID, QUEST_ID));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
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
