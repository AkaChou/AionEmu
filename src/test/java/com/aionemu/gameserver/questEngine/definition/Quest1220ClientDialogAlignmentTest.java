package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 1220「秘密配送」的宝箱交接链与 Aion 5.8 客户端按钮图一致：
 * 乌内(203172)接取并发箱 182200568 → 努蒙(798004)换箱 182200569 → 玛平恩恩(205240)交付结算。
 * 此前接取缺少发箱动作，导致 SETPRO1 因物品不足无法提交，服务层把按钮动作 ID
 * 当页面 ID 回显并触发客户端 load fail。
 * Verifies quest 1220 "A Secret Delivery" matches the Aion 5.8 client button graph:
 * Une (203172) starts and grants box 182200568, Numonerk (798004) exchanges it for 182200569,
 * Mappinerk (205240) settles the reward. The missing grant previously left the SETPRO1
 * transition without its required item, so the service layer echoed a button action id as a
 * page id and the client reported a load failure.
 */
class Quest1220ClientDialogAlignmentTest {
	private static final int START_NPC_ID = 203172;
	private static final int MID_NPC_ID = 798004;
	private static final int END_NPC_ID = 205240;
	private static final int BOX_FOR_NUMONERK = 182200568;
	private static final int BOX_FOR_MAPPINERK = 182200569;

	@Test
	void startGrantsTheTreasureBoxAndOnlyUneOwnsTheQuestStart() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "started1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		QuestTransition accept = talk(definition, "unaccepted", "started", START_NPC_ID,
			QuestDialogAction.QUEST_ACCEPT_1.id());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(BOX_FOR_NUMONERK, 1)), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());

		// 努蒙与玛平恩恩只承接对话，不是接取 NPC（对齐零售处理器仅 203172 addOnQuestStart）。
		// Numonerk and Mappinerk only own dialogs, not the acquisition (retail registers addOnQuestStart on 203172).
		for (int npcId : List.of(MID_NPC_ID, END_NPC_ID)) {
			assertFalse(definition.transitions().stream().anyMatch(transition ->
				"unaccepted".equals(transition.sourceNode()) && transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == npcId), "npc " + npcId + " must not start quest 1220");
		}
	}

	@Test
	void numonerkExchangesTheBoxAndMappinerkSettlesTheReward() throws Exception {
		QuestDefinition definition = definition().definition();

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			talk(definition, "started", "started", MID_NPC_ID, QuestDialogAction.QUEST_SELECT.id()).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			talk(definition, "started", "started", MID_NPC_ID, QuestDialogAction.SELECT2_1.id()).afterCommit());

		QuestTransition exchange = talk(definition, "started", "started1", MID_NPC_ID,
			QuestDialogAction.SETPRO1.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), exchange.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(BOX_FOR_NUMONERK, 1),
			new QuestAction.GiveItem(BOX_FOR_MAPPINERK, 1)), exchange.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), exchange.afterCommit());

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
			talk(definition, "started1", "started1", END_NPC_ID, QuestDialogAction.QUEST_SELECT.id()).afterCommit());

		QuestTransition report = talk(definition, "started1", "reward", END_NPC_ID,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		List<QuestTransition> completions = definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.toList();
		assertEquals(16, completions.size());
		for (QuestTransition completion : completions) {
			assertTrue(completion.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == END_NPC_ID,
				completion::toString);
			assertEquals(List.of(
				new QuestAction.GrantReward("GOLD", 0, 6620L, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 91950L, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 188100335, 14L, QuestRewardAmountMode.EXACT),
				new QuestAction.CompleteQuest(0)), completion.actions());
		}
	}

	@Test
	void treasureBoxesStayRegisteredAsQuestWorkItems() throws Exception {
		assertEquals(List.of(new QuestItemRequirement(BOX_FOR_NUMONERK, 1),
			new QuestItemRequirement(BOX_FOR_MAPPINERK, 1)),
			definition().definition().metadata().questWorkItems());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& candidate.event().equals(new QuestEvent.TalkToNpc(npcId, action)))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest1220ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1220.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1220.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
