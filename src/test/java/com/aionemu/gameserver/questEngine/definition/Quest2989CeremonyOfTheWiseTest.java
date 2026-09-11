package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the Asmodian ceremony dialog chain, reward owner, and class reward matrix.
 */
class Quest2989CeremonyOfTheWiseTest {

	private static final int START_NPC = 204146;
	private static final Set<PlayerClass> ADVANCED_CLASSES = Set.of(
		PlayerClass.GLADIATOR, PlayerClass.TEMPLAR, PlayerClass.RANGER, PlayerClass.ASSASSIN,
		PlayerClass.SORCERER, PlayerClass.SPIRIT_MASTER, PlayerClass.CLERIC, PlayerClass.CHANTER,
		PlayerClass.GUNSLINGER, PlayerClass.SONGWEAVER, PlayerClass.AETHERTECH);

	@Test
	void matchesLegacyClassDpMovieAndRewardContracts() throws Exception {
		QuestDefinition definition = definition();
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "stage-complete", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "choice", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "combat-report", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "class-report", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "reward-combat", QuestStatus.REWARD, Map.of("var0", 3));
		assertNode(definition, "reward-class", QuestStatus.REWARD, Map.of("var0", 4));

		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT1);
		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.ASK_QUEST_ACCEPT,
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW);

		assertStage(definition, new Stage(204056, PlayerClass.WARRIOR, QuestDialogPage.SELECT2,
			PlayerClass.SCOUT, QuestDialogPage.SELECT2_2, QuestDialogAction.SELECT2_1,
			QuestDialogPage.SELECT2_1, QuestDialogAction.SELECT2_1_1,
			QuestDialogPage.SELECT2_1_1));
		assertStage(definition, new Stage(204057, PlayerClass.SCOUT, QuestDialogPage.SELECT3,
			PlayerClass.WARRIOR, QuestDialogPage.SELECT3_2, QuestDialogAction.SELECT3_1,
			QuestDialogPage.SELECT3_1, QuestDialogAction.SELECT3_1_1,
			QuestDialogPage.SELECT3_1_1));
		assertStage(definition, new Stage(204058, PlayerClass.MAGE, QuestDialogPage.SELECT4,
			PlayerClass.WARRIOR, QuestDialogPage.SELECT4_2, QuestDialogAction.SELECT4_1,
			QuestDialogPage.SELECT4_1, QuestDialogAction.SELECT4_1_1,
			QuestDialogPage.SELECT4_1_1));
		assertStage(definition, new Stage(204059, PlayerClass.PRIEST, QuestDialogPage.SELECT5,
			PlayerClass.WARRIOR, QuestDialogPage.SELECT5_2, QuestDialogAction.SELECT5_1,
			QuestDialogPage.SELECT5_1, QuestDialogAction.SELECT5_1_1,
			QuestDialogPage.SELECT5_1_1));
		assertStage(definition, new Stage(801222, PlayerClass.TECHNIST, QuestDialogPage.SELECT5_3,
			PlayerClass.WARRIOR, QuestDialogPage.SELECT5_3_3, QuestDialogAction.SELECT5_3_1,
			QuestDialogPage.SELECT5_3_1, QuestDialogAction.SELECT5_3_2,
			QuestDialogPage.SELECT5_3_2));
		assertStage(definition, new Stage(801223, PlayerClass.MUSE, QuestDialogPage.SELECT5_4,
			PlayerClass.WARRIOR, QuestDialogPage.SELECT5_4_3, QuestDialogAction.SELECT5_4_1,
			QuestDialogPage.SELECT5_4_1, QuestDialogAction.SELECT5_4_2,
			QuestDialogPage.SELECT5_4_2));

		assertPage(definition, "stage-complete", START_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT6);
		assertPage(definition, "stage-complete", START_NPC, QuestDialogAction.SELECT6_1,
			QuestDialogPage.SELECT6_1);
		assertPage(definition, "stage-complete", START_NPC, QuestDialogAction.SELECT6_1_1,
			QuestDialogPage.SELECT6_1_1);
		assertTalk(definition, "stage-complete", "choice", START_NPC, QuestDialogAction.SETPRO2,
			List.of(), List.of(new QuestAction.SetVariable("var0", 2)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT7.id())));
		assertTalk(definition, "choice", "combat-report", START_NPC, QuestDialogAction.SETPRO4,
			List.of(), List.of(new QuestAction.SetVariable("var0", 3)), closeWithSync());
		assertTalk(definition, "choice", "class-report", START_NPC, QuestDialogAction.SETPRO5,
			List.of(), List.of(new QuestAction.SetVariable("var0", 4)), closeWithSync());
		assertDpBranch(definition, "combat-report", QuestDialogPage.SELECT8_2, QuestDialogPage.SELECT8);
		assertDpBranch(definition, "class-report", QuestDialogPage.SELECT9_2, QuestDialogPage.SELECT9);
		assertRewardTransition(definition, "combat-report", "reward-combat");
		assertRewardTransition(definition, "class-report", "reward-class");

		assertRewardMatrix(definition, "reward-combat");
		assertRewardMatrix(definition, "reward-class");
		assertTrue(definition.transitions().stream()
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
			.map(QuestTransition::event)
			.map(QuestEvent.TalkToNpc.class::cast)
			.allMatch(talk -> talk.npcId() == START_NPC));
	}

	private static void assertStage(QuestDefinition definition, Stage stage) {
		assertClassPage(definition, stage.npcId(), stage.permittedClass(), stage.permittedPage());
		assertClassPage(definition, stage.npcId(), stage.deniedClass(), stage.deniedPage());
		assertPage(definition, "started", stage.npcId(), stage.firstAction(), stage.firstPage());
		assertPage(definition, "started", stage.npcId(), stage.secondAction(), stage.secondPage());
		assertTalk(definition, "started", "stage-complete", stage.npcId(), QuestDialogAction.SETPRO1,
			List.of(), List.of(new QuestAction.SetVariable("var0", 1)), closeWithSync());
	}

	private static void assertClassPage(QuestDefinition definition, int npcId,
			PlayerClass playerClass, QuestDialogPage page) {
		QuestTransition transition = talk(definition, "started", npcId, QuestDialogAction.QUEST_SELECT,
			List.of(new QuestCondition.PlayerClassIs(playerClass)));
		assertEquals("started", transition.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertDpBranch(QuestDefinition definition, String source,
			QuestDialogPage belowMaxPage, QuestDialogPage maxPage) {
		assertPageTransition(definition, source, START_NPC, QuestDialogAction.QUEST_SELECT,
			List.of(new QuestCondition.CurrencyBelow(QuestRewardKind.DP, 4000)), belowMaxPage);
		assertPageTransition(definition, source, START_NPC, QuestDialogAction.QUEST_SELECT,
			List.of(new QuestCondition.CurrencyAtLeast(QuestRewardKind.DP, 4000)), maxPage);
	}

	private static void assertRewardTransition(QuestDefinition definition, String source, String target) {
		assertTalk(definition, source, target, START_NPC, QuestDialogAction.SELECT_QUEST_REWARD,
			List.of(), List.of(new QuestAction.SetCurrency(QuestRewardKind.DP, 0)),
			List.of(new AfterCommitAction.PlayMovie(137),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
	}

	private static void assertRewardMatrix(QuestDefinition definition, String source) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.toList();
		assertEquals(55, routes.size());
		for (int action = 8; action <= 12; action++) {
			int dialogAction = action;
			Set<PlayerClass> classes = routes.stream()
				.filter(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).dialogId() == dialogAction)
				.map(QuestTransition::conditions)
				.flatMap(List::stream)
				.filter(QuestCondition.AdvancedClassIs.class::isInstance)
				.map(QuestCondition.AdvancedClassIs.class::cast)
				.map(QuestCondition.AdvancedClassIs::playerClass)
				.collect(Collectors.toSet());
			assertEquals(ADVANCED_CLASSES, classes);
		}
		QuestTransition gladiator = routes.stream()
			.filter(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).dialogId() == 8)
			.filter(candidate -> candidate.conditions().equals(
				List.of(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR))))
			.findFirst()
			.orElseThrow();
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 437100, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 114600794, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), gladiator.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			gladiator.afterCommit());
	}

	private static List<AfterCommitAction> closeWithSync() {
		return List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertPageTransition(definition, source, npcId, action, List.of(), page);
	}

	private static void assertPageTransition(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, List<QuestCondition> conditions, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(source, transition.targetNode());
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

	private static QuestDefinition definition() throws Exception {
		try (InputStream input = Quest2989CeremonyOfTheWiseTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/2989.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 2989.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private record Stage(int npcId, PlayerClass permittedClass, QuestDialogPage permittedPage,
			PlayerClass deniedClass, QuestDialogPage deniedPage, QuestDialogAction firstAction,
			QuestDialogPage firstPage, QuestDialogAction secondAction, QuestDialogPage secondPage) {
	}
}
