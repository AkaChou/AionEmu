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
 * 验证任务 19637（[신발] 지역 몬스터 처치）的真端 DD 行（DD_TALK_HUNT_GRID）合同。
 * <p>
 * 已退役（保留清单 owner=RETAIL_TABLE，family=DataDriven）：旧「生产视图」IR 节点断言随 P7 步 f 退场，
 * 按计划 §8.9（P3 重锚口径）改锚 DD 运行时公共面：Talk 接取（Cainus，con_quest 19638 装载列）、
 * 击杀网格（4 变体 ×10 落组 1）、组槽达标收口进 REWARD、交付 NPC 的奖励窗面。
 * <p>
 * Re-anchored (plan §8.9) to the DD runtime faces: the Talk acquire row (Cainus), the ten-kill grid
 * (4 variants, group 1), the all-groups closure into REWARD and the delivery NPC's reward-window face.
 */
class Quest19637ClientDialogAlignmentTest {
	private static final int QUEST_ID = 19637;
	private static final int CAINUS_NPC_ID = 798926;
	private static final int OBJECT_ID = 900_037;
	private static final List<Integer> TARGET_MOBS = List.of(215500, 215501, 215502, 215503);
	private static final int KILLS_REQUIRED = 10;

	@Test
	void retailRowKeepsTheTalkAcquireAndTheTenKillGrid() throws Exception {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		assertTrue(runtime.owns(QUEST_ID), "19637 必须由 DD 运行时拥有");
		assertTrue(runtime.routes(QUEST_ID), "19637 必须由 DD 运行时路由");
		assertEquals(CAINUS_NPC_ID, NativeNpcNameResolver.instance().resolve("Cainus").npcIds().get(0));

		// 行数据：Talk 接取（acquireParam = Cainus）、交付 = 同一 NPC、con_quest 19638 装载列。
		DataDrivenQuestTable table = DataDrivenQuestTable.load(
			Quest19637ClientDialogAlignmentTest.class
				.getResourceAsStream(DataDrivenNativeRuntime.TABLE_RESOURCE));
		DataDrivenQuestTable.Row row = table.find(QUEST_ID).orElseThrow();
		assertEquals("talk", row.acquireKind(), "接取类别 = 真端 Talk 行");
		assertEquals("Cainus", row.acquireParam(), "接取 NPC 名 = 真端 value0_acquire_");
		assertEquals("Cainus", row.rewardNpc(), "交付 NPC 名 = 真端 reward_npc_name");
		assertEquals("19638", row.conQuest(), "con_quest 装载列");

		// 接取 talk 兴趣面注册本行（对象 #1 = 接取 NPC）。
		assertTrue(runtime.acquireTalkInterests().getOrDefault(CAINUS_NPC_ID, List.of()).contains(QUEST_ID),
			"Cainus 必须注册 19637 的接取谈话面");

		// 击杀网格：4 变体全部落组 1 的同一步。
		for (int mobId : TARGET_MOBS) {
			assertTrue(hasKillHit(runtime, mobId, 1), "击杀目标 " + mobId + " 必须落段 1 组槽");
		}
	}

	@Test
	void acceptGridWalkAndRewardFacesFollowTheRetailGrid() throws Exception {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 50);
		place(player, 0f, 0f, 0f);

		// 未接取：任务行打开真端接取入口页（4762 = 客户端声明的 select_none），带任务上下文。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, CAINUS_NPC_ID, 31, OBJECT_ID, QUEST_ID));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 4762, QUEST_ID);

		// 接取收尾（20000）：建档 START + 关窗（真端 20000 收尾不发对话页）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, CAINUS_NPC_ID, 20000, OBJECT_ID, QUEST_ID));
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.START, state.getStatus(), "接取收尾必须把任务建到 START");
		NativeTalkFixture.assertCloseDialog(player);

		// 网格推进：4 变体轮转；前 9 杀逐杀累加且保持 START，第 10 杀组槽达标 ⇒ 收口进 REWARD（清槽）。
		for (int kill = 1; kill < KILLS_REQUIRED; kill++) {
			assertTrue(kill(runtime, player, TARGET_MOBS.get((kill - 1) % TARGET_MOBS.size())),
				"第 " + kill + " 杀必须计数");
			assertEquals(kill, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1),
				"组 1 计数 " + kill);
			assertEquals(QuestStatus.START, state.getStatus(), "未满 10 杀不得进领奖");
		}
		assertTrue(kill(runtime, player, TARGET_MOBS.get((KILLS_REQUIRED - 1) % TARGET_MOBS.size())),
			"第 " + KILLS_REQUIRED + " 杀收口");
		assertEquals(QuestStatus.REWARD, state.getStatus(), "组槽达标 ⇒ 收口进领奖");
		assertEquals(0, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1), "收口清槽");

		// 领奖面：交付 NPC 上任务行/确认动作弹奖励窗（页 5，带任务上下文）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, CAINUS_NPC_ID, 31, OBJECT_ID, QUEST_ID));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, CAINUS_NPC_ID, 1009, OBJECT_ID, QUEST_ID));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);

		// 行数据一致性：真端元数据（minlevel=50、pc_light、EXP 与可选鞋奖励）。
		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(50, metadata.minLevel(), "真端 minlevel_permitted=50");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "真端 pc_light");
		assertTrue(metadata.rewards().contains(new QuestReward("EXP", 0, 6937236)),
			() -> metadata.rewards().toString());
		assertEquals(6, metadata.rewards().stream()
			.filter(reward -> reward.kind().equals("SELECTABLE_ITEM")).count(), "真端 6 件可选鞋奖励");
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
