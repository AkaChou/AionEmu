package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 锁定 2393、3722 的三秒物品读条、物品消耗和最终奖励 NPC 链。
 * Locks the three-second item casts, item consumption, and final reward-NPC chains for quests 2393 and 3722.
 */
class Quest2393And3722ItemPlayRewardOwnerTest {
	private static final int QUEST_2393 = 2393;
	private static final int QUEST_3722 = 3722;
	private static final int QUEST_4722 = 4722;
	private static final int FATHER = 204343;
	private static final int YANNIS = 799069;
	private static final int ASMODIAN_WEAPON_TESTER = 799403;
	private static final int FATHER_WATER = 182204162;
	private static final int FATHER_WARM_WATER = 182204163;
	private static final int YANNIS_TOY = 182202194;
	private static final int ASMODIAN_WEAPON = 182205692;
	private static final String FATHER_USE_ZONE = "DF2_ITEMUSEAREA_Q2393";
	private static final String YANNIS_USE_ZONE = "DDREADGION_02_ITEMUSEAREA_Q3722";

	@Test
	void quest2393PreservesTheItemPlayAndReturnToFatherContract() throws Exception {
		QuestDefinition definition = definition(QUEST_2393).definition();

		QuestTransition itemPlay = transition(definition, "started", "reward",
			new QuestEvent.ItemPlay(FATHER_WATER, 3_000));
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.ZoneIs(FATHER_USE_ZONE, true)), itemPlay.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(FATHER_WATER, 1),
			new QuestAction.GiveItem(FATHER_WARM_WATER, 1),
			new QuestAction.SetVariable("var0", 1)), itemPlay.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			itemPlay.afterCommit());

		assertPage(definition, "reward", FATHER, QuestDialogAction.USE_OBJECT, QuestDialogPage.DEFAULT_SUCCESS);
		assertPage(definition, "reward", FATHER, QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertNoCompletionRemoves(definition, FATHER);
	}

	@Test
	void quest3722PreservesTheItemPlayAndReturnToYannisContract() throws Exception {
		QuestDefinition definition = definition(QUEST_3722).definition();

		QuestTransition itemPlay = transition(definition, "started", "reward",
			new QuestEvent.ItemPlay(YANNIS_TOY, 3_000));
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.ZoneIs(YANNIS_USE_ZONE, true)), itemPlay.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(YANNIS_TOY, 1),
			new QuestAction.SetVariable("var0", 1)), itemPlay.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			itemPlay.afterCommit());

		assertPage(definition, "reward", YANNIS, QuestDialogAction.USE_OBJECT, QuestDialogPage.DEFAULT_SUCCESS);
		assertPage(definition, "reward", YANNIS, QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertNoCompletionRemoves(definition, YANNIS);
	}

	@Test
	void quest4722PreservesTheMirroredItemPlayAndRewardOwnerContract() throws Exception {
		QuestDefinition definition = definition(QUEST_4722).definition();

		QuestTransition itemPlay = transition(definition, "started", "reward",
			new QuestEvent.ItemPlay(ASMODIAN_WEAPON, 3_000));
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.ZoneIs(YANNIS_USE_ZONE, true)), itemPlay.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(ASMODIAN_WEAPON, 1),
			new QuestAction.SetVariable("var0", 1)), itemPlay.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			itemPlay.afterCommit());

		assertPage(definition, "reward", ASMODIAN_WEAPON_TESTER, QuestDialogAction.USE_OBJECT,
			QuestDialogPage.DEFAULT_SUCCESS);
		assertPage(definition, "reward", ASMODIAN_WEAPON_TESTER, QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertNoCompletionRemoves(definition, ASMODIAN_WEAPON_TESTER);
	}

	@Test
	void productionJourneysCompleteOnlyAfterTheItemPlayProtocolAndRewardOwner() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		for (int questId : List.of(QUEST_2393, QUEST_3722, QUEST_4722)) {
			CompiledQuestDefinition definition = definition(questId);
			QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
			assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));
			assertTrue(planned.plan().steps().stream().anyMatch(step ->
				step.kind() == QuestProductionJourneyPlanner.StepKind.ITEM_PLAY), planned.plan()::toString);

			QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
				.execute(definition, oracle, planned.plan());
			assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
		}
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, source, npcId, action);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertNoCompletionRemoves(QuestDefinition definition, int npcId) {
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode()))
			.filter(transition -> "complete".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
		assertFalse(completions.isEmpty());
		assertTrue(completions.stream().flatMap(transition -> transition.actions().stream())
			.noneMatch(QuestAction.RemoveItem.class::isInstance));
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
		assertEquals(1, routes.size(), "transition " + source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest2393And3722ItemPlayRewardOwnerTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
