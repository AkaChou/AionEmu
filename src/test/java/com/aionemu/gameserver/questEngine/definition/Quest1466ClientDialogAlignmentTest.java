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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 1466 的唯一接取 owner、3 秒物品读条与唯一奖励 NPC。
 * Verifies quest 1466's sole start owner, three-second item cast, and sole reward NPC.
 */
class Quest1466ClientDialogAlignmentTest {
	private static final int START_NPC_ID = 212649;
	private static final int REPORT_NPC_ID = 203903;
	private static final int WORK_ITEM_ID = 182201385;
	private static final String EXECUTION_GROUND = "EXECUTION_GROUND_OF_DELTRAS_220020000";
	private static final List<Integer> SELECTABLE_REWARD_IDS = List.of(
		125001887, 125001888, 125001889, 125001890);
	private static final List<QuestDialogAction> SELECTABLE_REWARD_ACTIONS = List.of(
		QuestDialogAction.SELECTED_QUEST_REWARD1,
		QuestDialogAction.SELECTED_QUEST_REWARD2,
		QuestDialogAction.SELECTED_QUEST_REWARD3,
		QuestDialogAction.SELECTED_QUEST_REWARD4);

	@Test
	void preservesTheRetailItemPlayRouteAndSoleRewardOwner() throws Exception {
		QuestDefinition definition = definition().definition();

		assertEquals(List.of(new QuestStartCondition("finished", 1465, 0)), definition.metadata().startConditions());
		assertEquals(Set.of("GLADIATOR", "TEMPLAR", "ASSASSIN", "RANGER", "SORCERER", "SPIRIT_MASTER",
			"CHANTER", "CLERIC", "PRIEST", "GUNSLINGER", "SONGWEAVER"), definition.metadata().permittedClasses());
		assertEquals(List.of(), definition.metadata().itemRequirements());
		assertEquals(List.of(new QuestItemRequirement(WORK_ITEM_ID, 1)), definition.metadata().questWorkItems());
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of());
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));
		assertTrue(routes(definition, "unaccepted", REPORT_NPC_ID).isEmpty());
		assertTrue(routes(definition, "reward", START_NPC_ID).isEmpty());

		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_ACCEPT_1,
				QuestDialogAction.QUEST_ACCEPT_SIMPLE)) {
			QuestTransition accept = route(definition, "unaccepted", START_NPC_ID, action);
			assertEquals("started", accept.targetNode());
			assertEquals(List.of(new QuestAction.GiveItem(WORK_ITEM_ID, 1)), accept.actions());
		}

		QuestTransition itemPlay = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.ItemPlay)
			.findFirst().orElseThrow();
		assertEquals("started", itemPlay.sourceNode());
		assertEquals("reward", itemPlay.targetNode());
		assertEquals(new QuestEvent.ItemPlay(WORK_ITEM_ID, 3_000), itemPlay.event());
		assertEquals(List.of(new QuestCondition.ZoneIs(EXECUTION_GROUND, true)), itemPlay.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(WORK_ITEM_ID, 1)), itemPlay.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			itemPlay.afterCommit());
		assertFalse(itemPlay.afterCommit().stream().anyMatch(AfterCommitAction.ShowQuestDialog.class::isInstance));

		assertPage(definition, "started", REPORT_NPC_ID, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
		QuestTransition report = route(definition, "started", REPORT_NPC_ID,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		assertTrue(routes(definition, "started", START_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty());

		for (int index = 0; index < SELECTABLE_REWARD_IDS.size(); index++) {
			assertCompletion(definition, SELECTABLE_REWARD_ACTIONS.get(index), SELECTABLE_REWARD_IDS.get(index));
		}
	}

	@Test
	void completesTheProductionHeadlessJourneyThroughTheItemPlayProtocol() throws Exception {
		CompiledQuestDefinition definition = definition();
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));
		assertTrue(planned.plan().steps().stream().anyMatch(step ->
			step.kind() == QuestProductionJourneyPlanner.StepKind.ITEM_PLAY), planned.plan()::toString);

		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	private static void assertCompletion(QuestDefinition definition, QuestDialogAction action,
			int selectableRewardId) {
		QuestTransition completion = route(definition, "reward", REPORT_NPC_ID, action);
		assertEquals("complete", completion.targetNode());
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 254850, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("AP", 0, 100, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("TITLE", 17, 1),
			new QuestAction.GrantReward("ITEM", 186000469, 2),
			new QuestAction.GrantReward("ITEM", selectableRewardId, 1),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())), completion.afterCommit());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest 1466 " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return routes(definition, source, npcId).stream()
			.filter(transition -> Integer.valueOf(action.id()).equals(
				((QuestEvent.TalkToNpc) transition.event()).dialogId()))
			.toList();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest1466ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1466.xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition 1466.xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
