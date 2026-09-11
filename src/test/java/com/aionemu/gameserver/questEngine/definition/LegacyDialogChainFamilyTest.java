package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the retail data-driven and legacy-handler dialog chains for quests 1463, 1901, and 2916.
 */
class LegacyDialogChainFamilyTest {

	@Test
	void messageToASpyMatchesTheDataDrivenTwoStepContract() throws Exception {
		QuestDefinition definition = definition(1463);
		assertEquals(List.of(new QuestItemRequirement(182201382, 1), new QuestItemRequirement(182201383, 1)),
			definition.metadata().questWorkItems());
		assertNode(definition, "s0", 0);
		assertNode(definition, "s1", 1);
		assertNode(definition, "reward", QuestStatus.REWARD, 2);
		assertStartContract(definition, 203940, "s0");

		assertPage(definition, "s0", 203903, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "s0", 203903, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertPage(definition, "s0", 203903, QuestDialogAction.SELECT2_2, QuestDialogPage.SELECT2_2);
		assertAdvance(definition, "s0", "s1", 203903, QuestDialogAction.SETPRO1,
			List.of(new QuestAction.GiveItem(182201382, 1)));

		assertPage(definition, "s1", 204424, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
		assertPage(definition, "s1", 204424, QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
		assertTalk(definition, "s1", "reward", 204424, QuestDialogAction.SETPRO2,
			List.of(), List.of(new QuestAction.GiveItem(182201383, 1),
				new QuestAction.RemoveItem(182201382, 1)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));

		assertRewardContract(definition, "reward", 203903, QuestDialogPage.SELECT5);
		assertSoleCompletionOwner(definition, 203903);
	}

	@Test
	void krallicPotionKeepsPaidAndItemExchangeBranchesOnTheLegacyOwners() throws Exception {
		QuestDefinition definition = definition(1901);
		assertEquals(List.of(new QuestItemRequirement(182206000, 1)),
			definition.metadata().questWorkItems());
		assertNode(definition, "s0", 0);
		assertNode(definition, "s1", 1);
		assertNode(definition, "s5", 5);
		assertNode(definition, "reward-paid", QuestStatus.REWARD, 5);
		assertNode(definition, "reward-alt", QuestStatus.REWARD, 6);
		assertStartContract(definition, 203830, "s0");

		assertPage(definition, "s0", 798026, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "s0", 798026, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertPage(definition, "s0", 798026, QuestDialogAction.SELECT2_1_1, QuestDialogPage.SELECT2_1_1);
		assertTalk(definition, "s0", "s0", 798026, QuestDialogAction.SELECT2_2,
			List.of(new QuestCondition.CurrencyAtLeast(QuestRewardKind.KINAH, 10000)),
			List.of(new QuestAction.DecreaseCurrency(QuestRewardKind.KINAH, 10000)),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_2.id())));
		assertTalk(definition, "s0", "s0", 798026, QuestDialogAction.SELECT2_3,
			List.of(new QuestCondition.CurrencyBelow(QuestRewardKind.KINAH, 10000)), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_3.id())));
		assertTalk(definition, "s0", "reward-paid", 798026, QuestDialogAction.SETPRO1,
			List.of(), List.of(new QuestAction.SetVariable("var0", 5)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));

		assertPage(definition, "s0", 798026, QuestDialogAction.SELECT2_3_1,
			QuestDialogPage.SELECT2_3_1);
		assertPage(definition, "s0", 798026, QuestDialogAction.SELECT2_3_1_1,
			QuestDialogPage.SELECT2_3_1_1);
		assertAdvance(definition, "s0", "s1", 798026, QuestDialogAction.SETPRO2,
			List.of(new QuestAction.SetVariable("var0", 1)));

		assertPage(definition, "s1", 798025, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
		assertPage(definition, "s1", 798025, QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
		assertAdvance(definition, "s1", "s2", 798025, QuestDialogAction.SETPRO3,
			List.of(new QuestAction.SetVariable("var0", 2)));
		assertPage(definition, "s2", 203131, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT4);
		assertPage(definition, "s2", 203131, QuestDialogAction.SELECT4_1, QuestDialogPage.SELECT4_1);
		assertPage(definition, "s2", 203131, QuestDialogAction.SELECT4_1_1,
			QuestDialogPage.SELECT4_1_1);
		assertAdvance(definition, "s2", "s3", 203131, QuestDialogAction.SETPRO4,
			List.of(new QuestAction.SetVariable("var0", 3)));
		assertPage(definition, "s3", 798003, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
		assertPage(definition, "s3", 798003, QuestDialogAction.SELECT5_1, QuestDialogPage.SELECT5_1);
		assertAdvance(definition, "s3", "s4", 798003, QuestDialogAction.SETPRO5,
			List.of(new QuestAction.SetVariable("var0", 4),
				new QuestAction.GiveItem(182206000, 1)));
		assertPage(definition, "s4", 798025, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT6);
		assertPage(definition, "s4", 798025, QuestDialogAction.SELECT6_1, QuestDialogPage.SELECT6_1);
		assertAdvance(definition, "s4", "s5", 798025, QuestDialogAction.SETPRO6,
			List.of(new QuestAction.SetVariable("var0", 5),
				new QuestAction.RemoveItem(182206000, 1)));
		assertPage(definition, "s5", 798026, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT7);
		assertPage(definition, "s5", 798026, QuestDialogAction.SELECT7_1, QuestDialogPage.SELECT7_1);
		assertTalk(definition, "s5", "reward-alt", 798026, QuestDialogAction.SETPRO7,
			List.of(), List.of(new QuestAction.SetVariable("var0", 6)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));

		assertRewardContract(definition, "reward-paid", 203864, QuestDialogPage.SELECT8);
		assertRewardContract(definition, "reward-alt", 203864, QuestDialogPage.SELECT8);
		assertSoleCompletionOwner(definition, 203864);
	}

	@Test
	void longBlackRobeRestoresTheDistanceDropAndTurnInContract() throws Exception {
		QuestDefinition definition = definition(2916);
		assertEquals(List.of(new QuestItemRequirement(182207007, 1)),
			definition.metadata().itemRequirements());
		assertEquals(List.of(new QuestDrop(700211, 182207007, 100, true, 6)),
			definition.metadata().drops());
		assertNode(definition, "s0", 0);
		assertNode(definition, "s5", 5);
		assertNode(definition, "s6", 6);
		assertNode(definition, "reward", QuestStatus.REWARD, 6);
		assertStartContract(definition, 204141, "s0");

		assertPage(definition, "s0", 204152, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "s0", 204152, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertAdvance(definition, "s0", "s1", 204152, QuestDialogAction.SETPRO1,
			List.of(new QuestAction.SetVariable("var0", 1)));
		assertPage(definition, "s1", 204150, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
		assertPage(definition, "s1", 204150, QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
		assertAdvance(definition, "s1", "s2", 204150, QuestDialogAction.SETPRO2,
			List.of(new QuestAction.SetVariable("var0", 2)));
		assertPage(definition, "s2", 204151, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT4);
		assertPage(definition, "s2", 204151, QuestDialogAction.SELECT4_1, QuestDialogPage.SELECT4_1);
		assertPage(definition, "s2", 204151, QuestDialogAction.SELECT4_1_1,
			QuestDialogPage.SELECT4_1_1);
		assertAdvance(definition, "s2", "s3", 204151, QuestDialogAction.SETPRO3,
			List.of(new QuestAction.SetVariable("var0", 3)));
		assertPage(definition, "s3", 798033, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
		assertPage(definition, "s3", 798033, QuestDialogAction.SELECT5_1, QuestDialogPage.SELECT5_1);
		assertAdvance(definition, "s3", "s4", 798033, QuestDialogAction.SETPRO4,
			List.of(new QuestAction.SetVariable("var0", 4)));
		assertPage(definition, "s4", 203673, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT6);
		assertPage(definition, "s4", 203673, QuestDialogAction.SELECT6_1, QuestDialogPage.SELECT6_1);
		assertAdvance(definition, "s4", "s5", 203673, QuestDialogAction.SETPRO5,
			List.of(new QuestAction.SetVariable("var0", 5)));

		QuestTransition distance = definition.transitions().stream()
			.filter(candidate -> "s5".equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.AtDistance distanceEvent
				&& distanceEvent.npcId() == 700211)
			.findFirst()
			.orElseThrow();
		assertEquals("s6", distance.targetNode());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 6)), distance.actions());
		assertTrue(definition.transitions().stream().anyMatch(candidate ->
			"s6".equals(candidate.sourceNode()) && "s6".equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == 700211
				&& "ACTION_ITEM_USE".equals(canAct.actionType())));

		assertPage(definition, "s6", 204141, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT7);
		assertTalk(definition, "s6", "reward", 204141,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
			List.of(new QuestCondition.HasItem(182207007, 1)),
			List.of(new QuestAction.RemoveItem(182207007, 1)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		assertTalk(definition, "s6", "s6", 204141,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
			List.of(new QuestCondition.HasItem(182207007, 1, false)), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT7_2.id())));

		assertRewardContract(definition, "reward", 204141,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertSoleCompletionOwner(definition, 204141);
	}

	private static void assertStartContract(QuestDefinition definition, int npcId,
			String targetNode) {
		assertPageTransition(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.QUEST_SELECT, List.of(), QuestDialogPage.SELECT1);
		assertPageTransition(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.ASK_QUEST_ACCEPT, List.of(),
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW);
		assertTalk(definition, "unaccepted", targetNode, npcId,
			QuestDialogAction.QUEST_ACCEPT_1,
			List.of(new QuestCondition.StartEligible()), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())));
	}

	private static void assertAdvance(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestAction> actions) {
		assertTalk(definition, source, target, npcId, action, List.of(), actions,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
	}

	private static void assertRewardContract(QuestDefinition definition, String source,
			int npcId, QuestDialogPage firstPage) {
		QuestTransition first = talk(definition, source, npcId, QuestDialogAction.QUEST_SELECT,
			List.of());
		assertEquals(source, first.targetNode());
		assertTrue(first.afterCommit().contains(
			new AfterCommitAction.ShowQuestDialog(firstPage.id())));
		QuestTransition selection = talk(definition, source, npcId,
			QuestDialogAction.SELECT_QUEST_REWARD, List.of());
		assertEquals(source, selection.targetNode());
		assertTrue(selection.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		QuestTransition completion = talk(definition, source, npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
	}

	private static void assertSoleCompletionOwner(QuestDefinition definition, int npcId) {
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
			.toList();
		assertTrue(!completions.isEmpty());
		assertTrue(completions.stream()
			.map(QuestTransition::event)
			.map(QuestEvent.TalkToNpc.class::cast)
			.allMatch(talk -> talk.npcId() == npcId));
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertPageTransition(definition, source, source, npcId, action, List.of(), page);
	}

	private static void assertPageTransition(QuestDefinition definition, String source,
			String target, int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			transition.afterCommit());
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

	private static void assertNode(QuestDefinition definition, String label, int var0) {
		assertNode(definition, label, QuestStatus.START, var0);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = LegacyDialogChainFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
