package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 39：三行「交付物品 -&gt; 对话/招供 -&gt; 向最终 NPC 报告并领奖」族（3013/3217/4217）。
 * <p>
 * 三只任务的客户端任务书都是三行、槽位 %0/%3/%6：行 0 交付收集物、行 1 和交付对象（3013 是让 Hecuba
 * 招供）继续对话、行 2 向最终 NPC 报告并领奖。客户端页链 `select1`(CHECK_USER_HAS_QUEST_ITEM) -&gt;
 * `check_user_item_ok`(SET_SUCCEED 或 FINISH_DIALOG) =&gt; 行 2 的 `select_success`(SELECT_QUEST_REWARD)。
 * 迁移把三行塌陷成 started(0) -&gt; reward(0) 的直跳，且 3217/4217 在交付 NPC 与报告 NPC 上各挂一份领奖，
 * 3013 把 798132 的报告页写成 Hecuba 的 SELECT2，导致行 1/行 2 没有状态。
 * </p>
 * <p>
 * Locks batch 39: the turn-in / talk / report family. Each journal row owns a state
 * (started=0, s1=1, reward=2); the turn-in admits row 1, SET_SUCCEED advances to row 2, and the final
 * report NPC owns the reward window.
 * </p>
 */
class Batch39TurnInTalkReportRowContractTest {

	private record TurnInQuest(int questId, int startNpc, int reportNpc, int itemId, int itemCount,
		int fixedRewardCount, int choiceCount) {
	}

	private static final List<TurnInQuest> FAMILY = List.of(
		new TurnInQuest(3013, 798132, 798132, 182208008, 1, 3, 2),
		new TurnInQuest(3217, 798335, 204590, 182209095, 3, 2, 0),
		new TurnInQuest(4217, 798336, 204773, 182209110, 3, 2, 0)
	);

	/** 3013 的行 1 由 Hecuba 798146 的三段对话承担；3217/4217 行 1 就在交付 NPC 上。 */
	private static final int HECUBA = 798146;

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		for (TurnInQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertNode(definition, contract.questId(), "started", QuestStatus.START, 0);
			assertNode(definition, contract.questId(), "s1", QuestStatus.START, 1);
			assertNode(definition, contract.questId(), "reward", QuestStatus.REWARD, 2);
			assertTrue(definition.progressLayout().field("var0").maxValue() >= 2,
				() -> "quest " + contract.questId() + " must widen var0 so all three rows fit");

			Set<Integer> rows = new LinkedHashSet<>();
			for (QuestNode candidate : definition.nodes()) {
				Integer row = candidate.projection().variables().get("var0");
				QuestStatus status = candidate.projection().status();
				if (row == null || row > 2 || (status != QuestStatus.START && status != QuestStatus.REWARD)) {
					continue;
				}
				rows.add(row);
			}
			assertEquals(Set.of(0, 1, 2), rows,
				() -> "quest " + contract.questId() + " owns a state per journal row");
		}
	}

	@Test
	void turnInAdmitsRowOneAndConsumesTheItem() throws Exception {
		for (TurnInQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();

			List<QuestTransition> admit = dialogRoutes(definition, "started", contract.startNpc(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM).stream()
				.filter(candidate -> "s1".equals(candidate.targetNode()))
				.toList();
			assertEquals(1, admit.size(),
				() -> "quest " + contract.questId() + " turn-in admit route must be unique");
			QuestTransition route = admit.getFirst();
			assertEquals("s1", route.targetNode(),
				() -> "quest " + contract.questId() + " turn-in must advance to row 1");
			assertEquals(List.of(new QuestCondition.HasItem(contract.itemId(), contract.itemCount(), true)),
				route.conditions(),
				() -> "quest " + contract.questId() + " turn-in must require the collected item");
			assertTrue(route.actions().contains(new QuestAction.SetVariable("var0", 1)),
				() -> "quest " + contract.questId() + " turn-in must project row 1: " + route.actions());
			assertTrue(route.actions().contains(
					new QuestAction.RemoveItem(contract.itemId(), contract.itemCount())),
				() -> "quest " + contract.questId() + " turn-in must consume the item: " + route.actions());
			assertTrue(route.afterCommit().contains(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				() -> "quest " + contract.questId() + " turn-in must refresh the journal");
			assertTrue(route.afterCommit().contains(
					new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
				() -> "quest " + contract.questId() + " turn-in must open the success page");

			List<QuestTransition> fail = dialogRoutes(definition, "started", contract.startNpc(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM).stream()
				.filter(candidate -> candidate.targetNode().equals("started"))
				.filter(candidate -> candidate.afterCommit().contains(
					new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())))
				.toList();
			assertEquals(1, fail.size(),
				() -> "quest " + contract.questId() + " must keep the item-check failure page");
		}
	}

	@Test
	void rowOneFinishesOnSetSucceedAndRowTwoOwnsTheReward() throws Exception {
		for (TurnInQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			int rowOneOwner = contract.questId() == 3013 ? HECUBA : contract.startNpc();

			List<QuestTransition> finish = dialogRoutes(definition, "s1", rowOneOwner,
				QuestDialogAction.SET_SUCCEED);
			assertEquals(1, finish.size(),
				() -> "quest " + contract.questId() + " row-1 finish route must be unique");
			assertEquals("reward", finish.getFirst().targetNode(),
				() -> "quest " + contract.questId() + " row-1 finish must reach the reward ladder");
			assertTrue(finish.getFirst().afterCommit().contains(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				() -> "quest " + contract.questId() + " row-1 finish must refresh visibility");

			List<QuestTransition> report = dialogRoutes(definition, "reward", contract.reportNpc(),
				QuestDialogAction.QUEST_SELECT);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.DEFAULT_SUCCESS.id())), report.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " row-2 report must open the client success page");
			List<QuestTransition> reward = dialogRoutes(definition, "reward", contract.reportNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD);
			assertEquals(1, reward.size(),
				() -> "quest " + contract.questId() + " reward route must be unique");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), reward.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " row-2 report must open reward window 1");

			assertCompletion(definition, contract);
		}
	}

	@Test
	void rowPagesAndConversationChainAreRouted() throws Exception {
		for (TurnInQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertPageRoute(definition, contract.startNpc(), "started",
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		}
		QuestDefinition hecuba = definition(3013).definition();
		assertPageRoute(hecuba, HECUBA, "s1", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPageRoute(hecuba, HECUBA, "s1", QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertPageRoute(hecuba, HECUBA, "s1", QuestDialogAction.SELECT2_1_1, QuestDialogPage.SELECT2_1_1);
	}

	@Test
	void staleRewardSaveHealsToRowTwo() throws Exception {
		for (TurnInQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> stale = definition.transitions().stream()
				.filter(route -> route.sourceNode() == null)
				.filter(route -> "reward".equals(route.targetNode()))
				.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", 0))))
				.toList();
			assertEquals(1, stale.size(),
				() -> "quest " + contract.questId() + " must heal the pre-migration reward save");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), stale.getFirst().actions(),
				() -> "quest " + contract.questId() + " heals to the report row");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), stale.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " refreshes the journal after healing");
		}
	}

	private static void assertCompletion(QuestDefinition definition, TurnInQuest contract) {
		int expectedRewardIndex = 0; // 三家都用 complete-reward-index=0 / all three keep completion index 0
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
			.toList();
		assertFalse(completions.isEmpty(),
			() -> "quest " + contract.questId() + " must complete from the reward row");
		for (QuestTransition completion : completions) {
			assertTrue(completion.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == contract.reportNpc(),
				() -> "quest " + contract.questId() + " completion must stay on the report NPC");
			assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(expectedRewardIndex)),
				() -> "quest " + contract.questId() + " must complete at reward index "
					+ expectedRewardIndex + " but had " + completion.actions());
		}
		if (contract.choiceCount() > 0) {
			List<QuestTransition> choiceRoutes = definition.transitions().stream()
				.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
				.filter(route -> route.actions().stream().anyMatch(QuestAction.GrantReward.class::isInstance))
				.toList();
			assertEquals(contract.choiceCount(), choiceRoutes.size(),
				() -> "quest " + contract.questId() + " must route every selectable reward choice");
		}
	}

	private static void assertNode(QuestDefinition definition, int questId, String label,
		QuestStatus status, int row) {
		QuestNode node = node(definition, label);
		assertEquals(status, node.projection().status(),
			() -> "quest " + questId + " node " + label + " status");
		assertEquals(row, node.projection().variables().get("var0"),
			() -> "quest " + questId + " node " + label + " row");
	}

	private static void assertPageRoute(QuestDefinition definition, int npcId, String source,
		QuestDialogAction action, QuestDialogPage page) {
		List<QuestTransition> routes = dialogRoutes(definition, source, npcId, action);
		assertEquals(1, routes.size(),
			() -> "route " + source + " + npc " + npcId + " + " + action + " must be unique");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			routes.getFirst().afterCommit(),
			() -> "route " + source + " + npc " + npcId + " + " + action + " page");
	}

	private static List<QuestTransition> dialogRoutes(QuestDefinition definition, String source,
		int npcId, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == action.id())
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch39TurnInTalkReportRowContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
