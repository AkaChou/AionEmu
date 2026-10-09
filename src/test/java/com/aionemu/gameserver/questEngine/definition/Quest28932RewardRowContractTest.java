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
 * 锁定 28932（[인던/파티] 함선 지휘관 처치，可重复）的原版 DD 行（Talk + 单杀网格）与领奖行。
 * Locks 28932's retail DD row (Talk + single-kill grid) and its reward row.
 * <p>
 * 已退役（保留清单 owner=RETAIL_TABLE，family=DataDriven）：旧「生产视图」网格节点/行号投影断言随
 * P7 步 f 退场，按计划 §8.9（P3 重锚口径）改锚 DD 运行时公共面：Talk 接取（IDDreadgion_04_Lenanti_E）、
 * 单杀网格（Dreadgion 德拉克忍者，落组 1）、一杀即收口进 REWARD、交付 NPC DF6_Olivia_E 的奖励窗面。
 * <p>
 * Re-anchored (plan §8.9) to the DD runtime faces: the Talk acquire row, the single-kill Drakan grid
 * (group 1), the one-kill closure into REWARD and the delivery NPC's reward-window face.
 */
class Quest28932RewardRowContractTest {
	private static final int QUEST_ID = 28932;
	private static final int LENANTI_NPC_ID = 806261;
	private static final int OLIVIA_NPC_ID = 806260;
	private static final int OBJECT_ID = 900_932;
	private static final String DRAKAN_NINJA = "IDDreadgion_04_DrakanWi_DrakanNinja_Ah";

	@Test
	void retailRowKeepsTheDreadgionAcquireAndTheSingleKillGrid() throws Exception {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		assertTrue(runtime.owns(QUEST_ID), "28932 必须由 DD 运行时拥有");
		assertTrue(runtime.routes(QUEST_ID), "28932 必须由 DD 运行时路由");
		assertTrue(NativeNpcNameResolver.instance().resolveMembers("IDDreadgion_04_Lenanti_E")
			.contains(LENANTI_NPC_ID), "接取 NPC 名必须解析到 806261");
		assertTrue(NativeNpcNameResolver.instance().resolveMembers("DF6_Olivia_E")
			.contains(OLIVIA_NPC_ID), "交付 NPC 名必须解析到 806260");

		DataDrivenQuestTable table = DataDrivenQuestTable.load(
			Quest28932RewardRowContractTest.class
				.getResourceAsStream(DataDrivenNativeRuntime.TABLE_RESOURCE));
		DataDrivenQuestTable.Row row = table.find(QUEST_ID).orElseThrow();
		assertEquals("talk", row.acquireKind(), "接取类别 = 原版 Talk 行");
		assertEquals("IDDreadgion_04_Lenanti_E", row.acquireParam(), "接取 NPC 名 = 原版 value0_acquire_");
		assertEquals("DF6_Olivia_E", row.rewardNpc(), "交付 NPC 名 = 原版 reward_npc_name");
		assertTrue(runtime.acquireTalkInterests().getOrDefault(LENANTI_NPC_ID, List.of()).contains(QUEST_ID),
			"Lenanti 必须注册 28932 的接取谈话面");

		// 单杀网格：德拉克忍者落组 1（原版 value0_progress_ 单行 1 杀）。
		List<Integer> ninjas = NativeNpcNameResolver.instance().resolveMonsterIds(DRAKAN_NINJA);
		assertTrue(!ninjas.isEmpty(), "德拉克忍者名必须可解析");
		for (int mobId : ninjas) {
			assertTrue(hasKillHit(runtime, mobId, 1), "忍者 " + mobId + " 必须落组 1 组槽");
		}

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(66, metadata.minLevel(), "原版 minlevel_permitted=66");
		assertEquals(java.util.Set.of("ASMODIANS"), metadata.permittedRaces(), "原版 pc_dark");
		assertEquals(255, metadata.repeatPolicy().maxRepeatCount(), "原版 max_repeat_count=255 ⇒ 可重复");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 7911702)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 188056947, 1)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 188056970, 1)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 166100011, 50)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 152012753, 1)), () -> rewards.toString());
	}

	@Test
	void singleKillSaturatesTheGridAndTheRewardFaceOpens() {
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		Player player = NativeTalkFixture.player(Race.ASMODIANS, PlayerClass.WARRIOR, 66);
		place(player, 0f, 0f, 0f);

		// 未接取：任务行打开原版接取入口页（4762 = 客户端声明的 select_none），带任务上下文。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, LENANTI_NPC_ID, 31, OBJECT_ID, QUEST_ID));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 4762, QUEST_ID);

		// 接取收尾（20000）：建档 START + 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, LENANTI_NPC_ID, 20000, OBJECT_ID, QUEST_ID));
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.START, state.getStatus(), "接取收尾必须把任务建到 START");
		NativeTalkFixture.assertCloseDialog(player);

		// 单杀网格：一杀即饱和 ⇒ 收口进 REWARD（清槽）。
		int ninjaId = NativeNpcNameResolver.instance().resolveMonsterIds(DRAKAN_NINJA).get(0);
		assertTrue(kill(runtime, player, ninjaId), "单杀必须计数并收口");
		assertEquals(QuestStatus.REWARD, state.getStatus(), "组槽达标 ⇒ 收口进领奖");
		assertEquals(0, DataDrivenProgress.counter(state.getQuestVars().getQuestVars(), 1), "收口清槽");

		// 领奖面：交付 NPC 上任务行弹奖励窗（页 5，带任务上下文）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(runtime.onDialog(player, OLIVIA_NPC_ID, 31, OBJECT_ID, QUEST_ID));
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
