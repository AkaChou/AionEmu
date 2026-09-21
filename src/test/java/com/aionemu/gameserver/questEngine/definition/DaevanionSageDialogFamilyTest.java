package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the mirrored Elyos/Asmodian sage dialog contracts for quests 1988 and 2988.
 */
class DaevanionSageDialogFamilyTest {

	private static final List<Case> CASES = List.of(
		new Case(1988, "ELYOS", 203725, 203989, 798018, 203771, true),
		new Case(2988, "ASMODIANS", 204182, 204338, 204213, 204146, false));

	@Test
	void mirroredSageQuestsKeepTheirThreeStageTurnInOwners() throws Exception {
		for (Case contract : CASES) {
			QuestDefinition definition = definition(contract.questId());
			assertEquals(List.of(new QuestItemRequirement(186000039, 1)),
				definition.metadata().itemRequirements());
			assertEquals(List.of(contract.race()), definition.metadata().permittedRaces().stream().toList());
			assertNode(definition, "s0", 0);
			assertNode(definition, "s1", 1);
			assertNode(definition, "s2", 2);
			// QE-051：客户端 QUEST_Q1988/Q2988 各 4 行，末行为领奖行，reward 投影 = 3（批次 1-7 已收口）。
			// QE-051: QUEST_Q1988/Q2988 each have 4 journal rows and the last one is the reward row, so the
			// REWARD projection is 3 (closed by batches 1-7).
			assertNode(definition, "reward", QuestStatus.REWARD, 3);
			assertStartContract(definition, contract.startNpc(), "s0");

			assertPage(definition, "s0", contract.firstNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
			assertPage(definition, "s0", contract.firstNpc(),
				QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
			assertAdvance(definition, "s0", "s1", contract.firstNpc(),
				QuestDialogAction.SETPRO1, 1);

			assertPage(definition, "s1", contract.secondNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
			assertPage(definition, "s1", contract.secondNpc(),
				QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
			if (contract.secondHasExtraPage()) {
				assertPage(definition, "s1", contract.secondNpc(),
					QuestDialogAction.SELECT3_1_1, QuestDialogPage.SELECT3_1_1);
			}
			assertAdvance(definition, "s1", "s2", contract.secondNpc(),
				QuestDialogAction.SETPRO2, 2);

			assertPage(definition, "s2", contract.finalNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT4);
			assertPage(definition, "s2", contract.finalNpc(),
				QuestDialogAction.SELECT4_1, QuestDialogPage.SELECT4_1);
			assertPage(definition, "s2", contract.finalNpc(),
				QuestDialogAction.SELECT4_2, QuestDialogPage.SELECT4_2);
			QuestTransition finish = talk(definition, "s2", contract.finalNpc(),
				QuestDialogAction.FINISH_DIALOG, List.of());
			assertEquals("s2", finish.targetNode());
			assertEquals(List.of(new AfterCommitAction.CloseDialog()), finish.afterCommit());

			QuestTransition turnIn = talk(definition, "s2", contract.finalNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD,
				List.of(new QuestCondition.HasItem(186000039, 1)));
			assertEquals("reward", turnIn.targetNode());
			assertEquals(List.of(new QuestAction.RemoveItem(186000039, 1)), turnIn.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				turnIn.afterCommit());

			QuestTransition reopen = talk(definition, "reward", contract.finalNpc(),
				QuestDialogAction.QUEST_SELECT, List.of());
			assertEquals("reward", reopen.targetNode());
			assertTrue(reopen.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
			assertSoleCompletionOwner(definition, contract.finalNpc());
		}
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
			int npcId, QuestDialogAction action, int var0) {
		assertTalk(definition, source, target, npcId, action, List.of(),
			List.of(new QuestAction.SetVariable("var0", var0)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
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
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source))
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
		try (InputStream input = DaevanionSageDialogFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private record Case(int questId, String race, int startNpc, int firstNpc, int secondNpc,
			int finalNpc, boolean secondHasExtraPage) {
	}
}
