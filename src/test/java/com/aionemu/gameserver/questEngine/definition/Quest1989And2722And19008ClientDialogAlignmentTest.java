package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the legacy handler and Aion 5.8 client dialog contracts for quests 1989, 2722, and 19008.
 */
class Quest1989And2722And19008ClientDialogAlignmentTest {

	@Test
	void quest1989MatchesLegacyClassDpAndRewardContract() throws Exception {
		QuestDefinition definition = definition(1989);
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "stage-complete", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "choice", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "combat-report", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "class-report", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "reward-combat", QuestStatus.REWARD, Map.of("var0", 3));
		assertNode(definition, "reward-class", QuestStatus.REWARD, Map.of("var0", 4));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));
		assertStartContract(definition, 203771, QuestDialogPage.SELECT1, "started");

		List<Stage> stages = List.of(
			new Stage(203704, PlayerClass.WARRIOR, QuestDialogPage.SELECT2, PlayerClass.SCOUT,
				QuestDialogPage.SELECT2_2, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1,
				QuestDialogAction.SELECT2_1_1, QuestDialogPage.SELECT2_1_1),
			new Stage(203705, PlayerClass.SCOUT, QuestDialogPage.SELECT3, PlayerClass.WARRIOR,
				QuestDialogPage.SELECT3_2, QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1,
				QuestDialogAction.SELECT3_1_1, QuestDialogPage.SELECT3_1_1),
			new Stage(203706, PlayerClass.MAGE, QuestDialogPage.SELECT4, PlayerClass.WARRIOR,
				QuestDialogPage.SELECT4_2, QuestDialogAction.SELECT4_1, QuestDialogPage.SELECT4_1,
				QuestDialogAction.SELECT4_1_1, QuestDialogPage.SELECT4_1_1),
			new Stage(203707, PlayerClass.PRIEST, QuestDialogPage.SELECT5, PlayerClass.WARRIOR,
				QuestDialogPage.SELECT5_2, QuestDialogAction.SELECT5_1, QuestDialogPage.SELECT5_1,
				QuestDialogAction.SELECT5_1_1, QuestDialogPage.SELECT5_1_1));
		for (Stage stage : stages) {
			assertClassPage(definition, stage, stage.permittedClass(), stage.permittedPage());
			assertClassPage(definition, stage, stage.deniedClass(), stage.deniedPage());
			assertPage(definition, "started", stage.npcId(), stage.firstAction(), stage.firstPage());
			assertPage(definition, "started", stage.npcId(), stage.secondAction(), stage.secondPage());
			assertStageCompletion(definition, stage.npcId());
		}

		assertClassPage(definition, 801214, PlayerClass.TECHNIST, QuestDialogPage.SELECT5_3_1_1);
		assertClassPage(definition, 801214, PlayerClass.WARRIOR, QuestDialogPage.SELECT5_3_2);
		assertStageCompletion(definition, 801214);
		assertClassPage(definition, 801215, PlayerClass.MUSE, QuestDialogPage.SELECT5_4_1_1);
		assertClassPage(definition, 801215, PlayerClass.WARRIOR, QuestDialogPage.SELECT5_4_2);
		assertStageCompletion(definition, 801215);

		assertPage(definition, "stage-complete", 203771, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT6);
		assertPage(definition, "stage-complete", 203771, QuestDialogAction.SELECT6_1,
			QuestDialogPage.SELECT6_1);
		assertPage(definition, "stage-complete", 203771, QuestDialogAction.SELECT6_1_1,
			QuestDialogPage.SELECT6_1_1);
		assertTalk(definition, "stage-complete", "choice", 203771, QuestDialogAction.SETPRO2,
			List.of(), List.of(new QuestAction.SetVariable("var0", 2)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT7.id())));

		assertPage(definition, "choice", 203771, QuestDialogAction.SELECT7_1, QuestDialogPage.SELECT7_1);
		assertPage(definition, "choice", 203771, QuestDialogAction.SELECT7_2, QuestDialogPage.SELECT7_2);
		assertTalk(definition, "choice", "combat-report", 203771, QuestDialogAction.SETPRO4,
			List.of(), List.of(new QuestAction.SetVariable("var0", 3)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "choice", "class-report", 203771, QuestDialogAction.SETPRO5,
			List.of(), List.of(new QuestAction.SetVariable("var0", 4)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));

		assertDpBranch(definition, "combat-report", QuestDialogPage.SELECT8_2, QuestDialogPage.SELECT8);
		assertDpBranch(definition, "class-report", QuestDialogPage.SELECT9_2, QuestDialogPage.SELECT9);
		assertRewardTransition(definition, "combat-report", "reward-combat");
		assertRewardTransition(definition, "class-report", "reward-class");
		assertCompletion(definition, "reward-combat");
		assertCompletion(definition, "reward-class");
	}

	@Test
	void quest2722MatchesLegacyEightStageNpcChainAndRewardOwner() throws Exception {
		QuestDefinition definition = definition(2722);
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		for (int stage = 0; stage <= 7; stage++) {
			assertNode(definition, "started" + stage, QuestStatus.START, Map.of("var0", stage));
		}
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 7));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));
		assertStartContract(definition, 278047, QuestDialogPage.SELECT_NONE, "started0");

		List<SimpleStage> stages = List.of(
			new SimpleStage(278056, QuestDialogPage.SELECT1, QuestDialogAction.SELECT1_1,
				QuestDialogPage.SELECT1_1, QuestDialogAction.SETPRO1),
			new SimpleStage(278126, QuestDialogPage.SELECT2, QuestDialogAction.SELECT2_1,
				QuestDialogPage.SELECT2_1, QuestDialogAction.SETPRO2),
			new SimpleStage(278043, QuestDialogPage.SELECT3, QuestDialogAction.SELECT3_1,
				QuestDialogPage.SELECT3_1, QuestDialogAction.SETPRO3),
			new SimpleStage(278032, QuestDialogPage.SELECT4, QuestDialogAction.SELECT4_1,
				QuestDialogPage.SELECT4_1, QuestDialogAction.SETPRO4),
			new SimpleStage(278037, QuestDialogPage.SELECT5, QuestDialogAction.SELECT5_1,
				QuestDialogPage.SELECT5_1, QuestDialogAction.SETPRO5),
			new SimpleStage(278040, QuestDialogPage.SELECT6, QuestDialogAction.SELECT6_1,
				QuestDialogPage.SELECT6_1, QuestDialogAction.SETPRO6),
			new SimpleStage(278068, QuestDialogPage.SELECT7, QuestDialogAction.SELECT7_1,
				QuestDialogPage.SELECT7_1, QuestDialogAction.SETPRO7),
			new SimpleStage(278066, QuestDialogPage.SELECT8, QuestDialogAction.SELECT8_1,
				QuestDialogPage.SELECT8_1, QuestDialogAction.SET_SUCCEED));
		for (int index = 0; index < stages.size(); index++) {
			SimpleStage stage = stages.get(index);
			String source = "started" + index;
			assertPage(definition, source, stage.npcId(), QuestDialogAction.QUEST_SELECT,
				stage.firstPage());
			assertPage(definition, source, stage.npcId(), stage.firstAction(), stage.secondPage());
			if (index < stages.size() - 1) {
				assertTalk(definition, source, "started" + (index + 1), stage.npcId(), stage.completionAction(),
					List.of(), List.of(new QuestAction.SetVariable("var0", index + 1)),
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
						new AfterCommitAction.CloseDialog()));
			} else {
				assertTalk(definition, source, "reward", stage.npcId(), stage.completionAction(),
					List.of(), List.of(new QuestAction.GiveItem(182205654, 1)),
					List.of(new AfterCommitAction.SyncQuestState(
							QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
						new AfterCommitAction.CloseDialog()));
			}
		}

		assertPage(definition, "reward", 278047, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.DEFAULT_SUCCESS);
		assertPage(definition, "reward", 278047, QuestDialogAction.USE_OBJECT,
			QuestDialogPage.DEFAULT_SUCCESS);
		QuestTransition preview = talk(definition, "reward", 278047,
			QuestDialogAction.SELECT_QUEST_REWARD, List.of());
		assertEquals("reward", preview.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		assertCompletionWithNpc(definition, "reward", 278047);
		assertTrue(definition.transitions().stream()
			.filter(candidate -> candidate.targetNode().equals("complete"))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
			.map(candidate -> (QuestEvent.TalkToNpc) candidate.event())
			.allMatch(talk -> talk.npcId() == 278047));
	}

	@Test
	void quest19008MatchesLegacyCraftingBranchAndItemTurnIn() throws Exception {
		QuestDefinition definition = definition(19008);
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started0", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "started1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));
		assertStartContract(definition, 203788, QuestDialogPage.SELECT_NONE, "started0");

		assertPage(definition, "started0", 203789, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT1);
		assertPage(definition, "started0", 203789, QuestDialogAction.SELECT1_1,
			QuestDialogPage.SELECT1_1);
		assertPage(definition, "started0", 203789, QuestDialogAction.SELECT1_2,
			QuestDialogPage.SELECT1_2);
		assertPage(definition, "started0", 203789, QuestDialogAction.SELECT1_3,
			QuestDialogPage.SELECT1_3);
		assertTalk(definition, "started0", "started1", 203789, QuestDialogAction.SETPRO10,
			List.of(), List.of(
				new QuestAction.GiveItem(152201706, 1),
				new QuestAction.GiveItem(152020250, 1),
				new QuestAction.SetVariable("var0", 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "started0", "started1", 203789, QuestDialogAction.SETPRO20,
			List.of(), List.of(
				new QuestAction.GiveItem(152201707, 1),
				new QuestAction.GiveItem(152020250, 1),
				new QuestAction.SetVariable("var0", 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));

		assertPage(definition, "started0", 203788, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.CHECK_USER_ITEM_FAIL);
		assertTalk(definition, "started1", "reward", 203788, QuestDialogAction.QUEST_SELECT,
			List.of(new QuestCondition.HasItem(182206764, 1, true)),
			List.of(new QuestAction.RemoveItem(182206764, 1)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())));
		assertTalk(definition, "started1", "started1", 203788, QuestDialogAction.QUEST_SELECT,
			List.of(new QuestCondition.HasItem(182206764, 1, false)),
			List.of(), List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.CHECK_USER_ITEM_FAIL.id())));

		QuestTransition preview = talk(definition, "reward", 203788,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, List.of());
		assertEquals("reward", preview.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		assertCompletionWithNpc(definition, "reward", 203788);
	}

	private static void assertStartContract(QuestDefinition definition, int npcId,
			QuestDialogPage startPage, String targetNode) {
		assertPageTransition(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.QUEST_SELECT, List.of(), startPage);
		assertPageTransition(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.ASK_QUEST_ACCEPT, List.of(), QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW);
		assertTalk(definition, "unaccepted", targetNode, npcId, QuestDialogAction.QUEST_ACCEPT_1,
			List.of(new QuestCondition.StartEligible()), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())));
	}

	private static void assertClassPage(QuestDefinition definition, Stage stage,
			PlayerClass playerClass, QuestDialogPage page) {
		assertClassPage(definition, stage.npcId(), playerClass, page);
	}

	private static void assertClassPage(QuestDefinition definition, int npcId,
			PlayerClass playerClass, QuestDialogPage page) {
		assertPageTransition(definition, "started", "started", npcId, QuestDialogAction.QUEST_SELECT,
			List.of(new QuestCondition.PlayerClassIs(playerClass)), page);
	}

	private static void assertStageCompletion(QuestDefinition definition, int npcId) {
		assertTalk(definition, "started", "stage-complete", npcId, QuestDialogAction.SETPRO1,
			List.of(), List.of(new QuestAction.SetVariable("var0", 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
	}

	private static void assertDpBranch(QuestDefinition definition, String source,
			QuestDialogPage belowMaxPage, QuestDialogPage maxPage) {
		assertPageTransition(definition, source, source, 203771, QuestDialogAction.QUEST_SELECT,
			List.of(new QuestCondition.CurrencyBelow(QuestRewardKind.DP, 4000)), belowMaxPage);
		assertPageTransition(definition, source, source, 203771, QuestDialogAction.QUEST_SELECT,
			List.of(new QuestCondition.CurrencyAtLeast(QuestRewardKind.DP, 4000)), maxPage);
	}

	private static void assertRewardTransition(QuestDefinition definition, String source, String target) {
		assertTalk(definition, source, target, 203771, QuestDialogAction.SELECT_QUEST_REWARD,
			List.of(), List.of(new QuestAction.SetCurrency(QuestRewardKind.DP, 0)),
			List.of(new AfterCommitAction.PlayMovie(105),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
	}

	private static void assertCompletion(QuestDefinition definition, String source) {
		QuestTransition completion = talk(definition, source, 203771,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertCompletionWithNpc(QuestDefinition definition, String source, int npcId) {
		QuestTransition completion = talk(definition, source, npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertPageTransition(definition, source, source, npcId, action, List.of(), page);
	}

	private static void assertPageTransition(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertTalk(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, List<QuestCondition> conditions) {
		QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(npcId, action.id());
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> candidate.conditions().equals(conditions))
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"missing route " + source + " + NPC " + npcId + " + action " + action.id()));
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest1989And2722And19008ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private record Stage(int npcId, PlayerClass permittedClass, QuestDialogPage permittedPage,
			PlayerClass deniedClass, QuestDialogPage deniedPage, QuestDialogAction firstAction,
			QuestDialogPage firstPage, QuestDialogAction secondAction, QuestDialogPage secondPage) {
	}

	private record SimpleStage(int npcId, QuestDialogPage firstPage, QuestDialogAction firstAction,
			QuestDialogPage secondPage, QuestDialogAction completionAction) {
	}
}
