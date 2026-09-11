package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the mirrored data-driven report-to-many quests 18805 and 28805.
 */
class ReportToManyMirrorQuestFamilyTest {

	private static final List<Case> CASES = List.of(
		new Case(18805, "ELYOS", 830070, 830520, 730522),
		new Case(28805, "ASMODIANS", 830154, 830521, 730525));

	@Test
	void mirroredReportToManyChainsKeepTheirStageOwners() throws Exception {
		for (Case contract : CASES) {
			QuestDefinition definition = definition(contract.questId());
			assertEquals(List.of(contract.race()), definition.metadata().permittedRaces().stream().toList());
			assertEquals(List.of(
				new QuestReward("EXP", 0, 26868),
				new QuestReward("ITEM", 170190060, 1)), definition.metadata().rewards());
			assertNode(definition, "s0", 0);
			assertNode(definition, "s1", 1);
			assertNode(definition, "reward", QuestStatus.REWARD, 1);
			assertStartContract(definition, contract.startNpc());

			assertPage(definition, "s0", contract.firstNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
			assertTalk(definition, "s0", "s1", contract.firstNpc(),
				QuestDialogAction.SETPRO1,
				List.of(), List.of(new QuestAction.SetVariable("var0", 1)),
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog()));

			assertPage(definition, "s1", contract.secondNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
			assertTalk(definition, "s1", "reward", contract.secondNpc(),
				QuestDialogAction.SETPRO2,
				List.of(), List.of(new QuestAction.SetVariable("var0", 1)),
				List.of(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog()));

			assertPage(definition, "reward", contract.firstNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
			QuestTransition preview = talk(definition, "reward", contract.firstNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD, List.of());
			assertEquals("reward", preview.targetNode());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
			assertSoleCompletionOwner(definition, contract.firstNpc());
		}
	}

	private static void assertStartContract(QuestDefinition definition, int npcId) {
		assertPageTransition(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.QUEST_SELECT, List.of(), QuestDialogPage.SELECT1);
		assertTalk(definition, "unaccepted", "s0", npcId,
			QuestDialogAction.QUEST_ACCEPT_SIMPLE,
			List.of(new QuestCondition.StartEligible()), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.QUEST_REFUSE_SIMPLE, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));
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
		assertTrue(completions.stream()
			.allMatch(candidate -> candidate.actions().stream()
				.anyMatch(QuestAction.CompleteQuest.class::isInstance)));
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
		try (InputStream input = ReportToManyMirrorQuestFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private record Case(int questId, String race, int startNpc, int firstNpc, int secondNpc) {
	}
}
