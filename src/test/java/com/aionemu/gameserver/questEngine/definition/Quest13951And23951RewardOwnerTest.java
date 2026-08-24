package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 锁定两条投递信件任务的唯一接取、领奖归属和原生奖励协议。
 * Locks unique acceptance, reward ownership, and native reward protocol for two letter-delivery quests.
 */
class Quest13951And23951RewardOwnerTest {
	@Test
	void keepsTheStartNpcAndRewardNpcAsSeparateOwners() throws Exception {
		assertContract(13951, 731784, 806582, 182216201);
		assertContract(23951, 731784, 806591, 182216202);
	}

	@Test
	void productionJourneysReachTheRetailRewardOwners() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		for (int questId : List.of(13951, 23951)) {
			CompiledQuestDefinition definition = definition(questId);
			QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
			assertTrue(planned.planned(), () -> questId + " " + planned.failure());
			assertTrue(planned.plan().steps().stream().anyMatch(step ->
				step.kind() == QuestProductionJourneyPlanner.StepKind.USE_OBJECT));
			assertTrue(planned.plan().steps().stream().anyMatch(step ->
				step.kind() == QuestProductionJourneyPlanner.StepKind.NATIVE_REWARD_ACTION));

			QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
				.execute(definition, oracle, planned.plan());
			assertTrue(executed.completed(), () -> questId + " " + executed.failure());
		}
	}

	private static void assertContract(int questId, int startNpc, int rewardNpc, int itemId) throws Exception {
		QuestDefinition definition = definition(questId).definition();
		assertTrue(routes(definition, "unaccepted", rewardNpc).isEmpty());
		assertTrue(routes(definition, "reward", startNpc).isEmpty());

		QuestTransition accept = talk(definition, "unaccepted", "started", startNpc, QuestDialogAction.QUEST_ACCEPT_SIMPLE);
		assertEquals(List.of(new QuestAction.GiveItem(itemId, 1)), accept.actions());

		QuestTransition itemUse = transition(definition, "started", "reward", new QuestEvent.UseItem(itemId));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), itemUse.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), itemUse.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), itemUse.afterCommit());

		QuestTransition useObject = talk(definition, "reward", "reward", rewardNpc, QuestDialogAction.USE_OBJECT);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			useObject.afterCommit());
		QuestTransition report = talk(definition, "reward", "reward", rewardNpc, QuestDialogAction.SELECT_QUEST_REWARD);
		assertTrue(report.actions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), report.afterCommit());

		QuestTransition completion = talk(definition, "reward", "complete", rewardNpc,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 405720, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 35561094, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), completion.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest13951And23951RewardOwnerTest.class
			.getResourceAsStream("/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
