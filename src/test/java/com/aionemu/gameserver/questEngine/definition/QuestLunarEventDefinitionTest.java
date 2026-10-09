package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 80034-80037 农历（구정 / LUNAR）事件任务：原版表行 = 单步 NPC 接取 + {@code item_check} 交付门
 * （{@code check_item1_1} 与 {@code collect_item1} 同值）+ 交付领奖，四行的种族/前置/交付物各不相同。
 * <p>
 * P3 重锚（计划 §8.9）：本类只断言原版表行、原版 {@code quest.xml} 与客户端页契约可实证的事实。
 * 旧 IR 断言的「onLvlUp 背包达标自动接取」「LUNAR bonus 事件保持状态」「活动失效弃任」只存在于本地 XML
 * 与旧 handler：原版 codegen 对这四行注册的是普通 SimpleTalk 槽位（{@code event_Harmonan}/{@code event_Druike}
 * NPC 节点 + cab520 接取 thunk + cabb10 对话 thunk），活动任务子系表 {@code quest/event_quest.xml} 全域缺失
 * （计划 §10.3-#3）⇒ 那些轴登记为**不可实证假设**，不再作为断言面，也不在 native 车道发明。
 * <p>
 * Lunar event quests 80034-80037: the retail row is a single-step NPC-acquired talk row with an
 * {@code item_check} hand-in gate equal to the collection item. P3 re-anchor (plan §8.9) keeps only
 * retail-table, quest.xml and client-contract facts; the old IR-only level-up auto-accept and LUNAR
 * bonus axes came from the local XML / old handlers and stay registered as unverifiable assumptions.
 */
class QuestLunarEventDefinitionTest {

	/** 原版行事实：{@code Quest_SimpleTalk.xml} 的 NPC 列 + {@code quest.xml} 的门/种族/前置列。 */
	private record LunarQuest(int questId, int npcId, int gateItemId, int gateCount, Race race,
			String prerequisite) {
	}

	/** 80034-80036 天族（event_Harmonan 799765），80037 魔族（event_Druike 799780）。 */
	private static final List<LunarQuest> QUESTS = List.of(
		new LunarQuest(80034, 799765, 164002016, 10, Race.ELYOS, "Q80029"),
		new LunarQuest(80035, 799765, 164002017, 5, Race.ELYOS, "Q80029"),
		new LunarQuest(80036, 799765, 164002018, 1, Race.ELYOS, "Q80029"),
		new LunarQuest(80037, 799780, 164002016, 10, Race.ASMODIANS, "Q80032"));

	@Test
	void retailRowsCarryTheSingleStepItemCheckHandIn() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (LunarQuest quest : QUESTS) {
			int questId = quest.questId();
			assertTrue(handler.routes(questId), "quest " + questId + " 必须由 native 车道路由");
			assertEquals(RetailGrantKind.NPC, handler.grantKind(questId), "NPC 接取行");
			assertEquals(quest.npcId(), handler.acquireNpc(questId), "接取 NPC（原版 acquired_npc_name）");
			assertEquals(quest.npcId(), handler.rewardNpc(questId), "交付 NPC（原版同主）");
			assertEquals(0, handler.relayCount(questId), "无中继步");
			assertEquals(List.of(new SimpleTalkHandler.ItemStack(quest.gateItemId(), quest.gateCount())),
				handler.workItems(questId), "交付门 = check_item1_1 工作物品");
			assertFalse(handler.unresolvedGate(questId), "交付门必须可解");
			// 原版行无 give_item：活动收集物由活动自身产出，接取不发放。
			assertNull(handler.acceptGiveItem(questId), "无接取发放");
			assertNull(handler.stepGiveItem(questId, 1), "无步进发放");
			assertNull(handler.stepRemoveItem(questId, 1), "无步进扣除");
			assertNull(handler.cutscene(questId), "无过场");
			assertTrue(NativeTalkFixture.clientDeclares(questId, SimpleTalkHandler.PAGE_ASK_ACCEPT),
				"客户端任务页声明原版接取窗页 4");
		}
	}

	@Test
	void questXmlDeclaresTheLunarBranchAxes() {
		for (LunarQuest quest : QUESTS) {
			NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(quest.questId()).orElseThrow();
			String gateText = row.text("collect_item1");
			assertEquals("event", row.text("category1"), "原版类别");
			assertEquals(10, row.integer("minlevel_permitted"), "原版等级下限");
			assertEquals(255, row.integer("max_repeat_count"), "原版重复上限（活动可重复）");
			assertEquals(quest.race() == Race.ELYOS ? "pc_light" : "pc_dark", row.text("race_permitted"),
				"原版种族轴");
			assertEquals(quest.prerequisite(), row.text("finished_quest_cond1"), "原版前置任务");
			assertEquals(gateText, row.text("check_item1_1"), "交付门与收集列同值（原版原始形态）");
			assertEquals(quest.gateCount(), Integer.parseInt(gateText.trim().split("\\s+")[1]), "交付数量");
			assertEquals("0", row.text("reward_exp1"), "经验由 LUNAR bonus 子系下发：quest.xml 为 0");
			assertEquals("0", row.text("reward_gold1"), "金币由 LUNAR bonus 子系下发：quest.xml 为 0");
		}
		assertEquals("%Quest_A_BranchLunarEvent_10a", questRow(80034).text("reward_item1_1"),
			"原版奖励列为活动子系占位符号 10a");
		assertEquals("%Quest_A_BranchLunarEvent_10b", questRow(80035).text("reward_item1_1"), "占位符号 10b");
		assertEquals("%Quest_A_BranchLunarEvent_10c", questRow(80036).text("reward_item1_1"), "占位符号 10c");
		assertEquals("%Quest_A_BranchLunarEvent_10a", questRow(80037).text("reward_item1_1"),
			"魔族 80037 复用 10a 档");
	}

	@Test
	void acceptAndHandInFollowTheRetailTalkLane() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (LunarQuest quest : QUESTS) {
			int questId = quest.questId();
			int prerequisite = Integer.parseInt(quest.prerequisite().substring(1));
			NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
			SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);
			Player player = NativeTalkFixture.player(quest.race(), PlayerClass.WARRIOR, 10);

			// 前置未完成：原版 finished_quest_cond1 未满足 ⇒ 拒接建档。
			assertEquals(NativeQuestStartPort.Outcome.PREREQUISITE_MISSING,
				NativeQuestStartPort.instance().evaluateNpcAcquire(player, questId).outcome(),
				"quest " + questId + " 前置未完成必须拒接");
			NativeTalkFixture.clearPackets(player);
			assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, quest.npcId(), questId, 1002)),
				"前置未完成不得建档");
			assertNull(player.getQuestStateList().getQuestState(questId), "拒接不得落库");

			NativeTalkFixture.completePrerequisites(player, prerequisite);
			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, quest.npcId(), questId, 31)), "接取问询");
			assertNull(player.getQuestStateList().getQuestState(questId), "问询页不得落库");
			NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(questId));

			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, quest.npcId(), questId, 1002)), "接取确认");
			assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus());
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);
			assertEquals(List.of(), inventory.calls(), "原版行无 give_item：接取不发物品");

			// 未集齐交付物：报告门保持 START（进行中页）。 / Without the items the gate holds.
			NativeTalkFixture.clearPackets(player);
			assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, quest.npcId(), questId, 1009)), "报告被受理");
			assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus(),
				"交付门未持有必须保持 START");
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_IN_PROGRESS);

			// 集齐：报告 → 领奖态并扣除整组。 / With the items the report pays into REWARD.
			inventory.hold(quest.gateItemId(), quest.gateCount());
			NativeTalkFixture.clearPackets(player);
			assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, quest.npcId(), questId, 1009)), "交付报告");
			assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(questId).getStatus());
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
			assertEquals(List.of("remove:" + quest.gateItemId() + ":" + quest.gateCount()), inventory.calls(),
				"交付门按原版扣除整组");
		}
	}

	@Test
	void maxRepeatCountKeepsTheEventReopenable() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (LunarQuest quest : QUESTS) {
			Player player = NativeTalkFixture.player(quest.race(), PlayerClass.WARRIOR, 10);
			// COMPLETE 态重开窗仍走同一 CanAcquireQuest 资格轴（前置必须已满足，否则不进接取面）；
			// 本判据隔离的是重复轴，故先按原版前置建档。
			// Reopening at COMPLETE passes the same CanAcquireQuest axes; this judgement isolates the
			// repeat axis, so the retail prerequisite is completed first.
			NativeTalkFixture.completePrerequisites(player, Integer.parseInt(quest.prerequisite().substring(1)));
			NativeTalkFixture.add(player, quest.questId(), QuestStatus.COMPLETE, 0);
			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, quest.npcId(), quest.questId, 31)),
				"quest " + quest.questId() + " max_repeat_count=255 ⇒ COMPLETE 后仍开接取窗");
			NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(quest.questId()));
		}
	}

	@Test
	void theRetailRowHasNoSystemGrantOrChainAxis() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (LunarQuest quest : QUESTS) {
			int questId = quest.questId();
			// 原版 codegen 注册的是 NPC 节点，活动子系默认槽不在 retail codegen 面内。
			assertFalse(handler.isSystemGranted(questId), "quest " + questId + " 只能由 NPC 接取");
			assertEquals(0, handler.factionId(questId), "quest " + questId + " 不是阵营日常行");
			assertNull(handler.conQuest(questId), "quest " + questId + " 无链式接取窗");
		}
	}

	@Test
	void theOppositeRaceCannotAcquireTheBranch() {
		// 80037 是魔族（pc_dark）行：天族接取必须 fail-closed；80034 反向同理。
		Player elyos = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 10);
		Player asmodian = NativeTalkFixture.player(Race.ASMODIANS, PlayerClass.WARRIOR, 10);
		NativeTalkFixture.completePrerequisites(elyos, 80032);
		NativeTalkFixture.completePrerequisites(asmodian, 80029);
		assertEquals(NativeQuestStartPort.Outcome.RACE_BLOCKED,
			NativeQuestStartPort.instance().evaluateNpcAcquire(elyos, 80037).outcome(), "80037 拒天族");
		assertEquals(NativeQuestStartPort.Outcome.RACE_BLOCKED,
			NativeQuestStartPort.instance().evaluateNpcAcquire(asmodian, 80034).outcome(), "80034 拒魔族");
	}

	private static NativeQuestXmlTable.QuestRow questRow(int questId) {
		return NativeQuestXmlTable.instance().find(questId).orElseThrow();
	}
}
