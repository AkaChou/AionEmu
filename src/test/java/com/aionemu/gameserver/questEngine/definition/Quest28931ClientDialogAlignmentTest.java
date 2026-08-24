package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 28931 的两次击杀、补给品清理和 Aion 5.8 客户端奖励链。
 * Verifies quest 28931's two-kill counter, supply cleanup, and Aion 5.8 client reward flow.
 */
class Quest28931ClientDialogAlignmentTest {
	private static final int CONVOY_OFFICER_ID = 243797;
	private static final int REWARD_NPC_ID = 806260;
	private static final int SUPPLY_ITEM_ID = 182213556;

	@Test
	void restoresLegacyTwoKillAndSingleSupplyRemovalContract() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 2, "var1", 0));

		QuestEvent kill = new QuestEvent.KillNpc(CONVOY_OFFICER_ID);
		QuestTransition firstKill = transition(definition, "s1", "s1", kill);
		assertEquals(Integer.valueOf(1), firstKill.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 1),
			new QuestCondition.VariableBelow("var1", 1)), firstKill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), firstKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			firstKill.afterCommit());

		QuestTransition secondKill = transition(definition, "s1", "reward", kill);
		assertEquals(Integer.valueOf(0), secondKill.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 1),
			new QuestCondition.VariableAtLeast("var1", 1)), secondKill.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 2),
			new QuestAction.SetVariable("var1", 0)), secondKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), secondKill.afterCommit());

		for (int action : List.of(QuestDialogAction.USE_OBJECT.id(),
				QuestDialogAction.SELECT_QUEST_REWARD.id())) {
			QuestTransition preview = talk(definition, "reward", "reward", action);
			assertEquals(List.of(new QuestAction.RemoveItem(SUPPLY_ITEM_ID, QuestAction.RemoveItem.ALL)),
				preview.actions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		}

		QuestTransition completion = talk(definition, "reward", "complete",
			QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		assertFalse(completion.actions().stream().anyMatch(QuestAction.RemoveItem.class::isInstance));
		assertTrue(completion.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	@Test
	void completesThroughTheProductionHeadlessJourney() throws Exception {
		CompiledQuestDefinition definition = definition();
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));

		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(REWARD_NPC_ID, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/28931.xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition 28931.xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
