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
 * Locks the client-paged three-stage report quests 15550 and 25550.
 */
class ThreeStageReportClientPathTest {

	private static final List<Case> CASES = List.of(
		new Case(15550, "ELYOS", 806114, 806113, 806134, 806089),
		new Case(25550, "ASMODIANS", 806116, 806115, 806135, 806101));

	@Test
	void clientPagedReportsUseTheObservedButtonIdsAndOwners() throws Exception {
		for (Case contract : CASES) {
			QuestDefinition definition = definition(contract.questId());
			assertEquals(List.of(contract.race()), definition.metadata().permittedRaces().stream().toList());
			assertNode(definition, "s0", 0);
			assertNode(definition, "s1", 1);
			// QE-051：客户端 quest_q15550/q25550 各 3 行，末行为领奖行，reward 投影 = 2（批次 5 已收口）。
			// QE-051: quest_q15550/q25550 each have 3 journal rows and the last one is the reward row, so the
			// REWARD projection is 2 (closed by batch 5).
			assertNode(definition, "reward", QuestStatus.REWARD, 2);
			assertStartContract(definition, contract.startNpc());

			assertPage(definition, "s0", contract.firstNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
			assertPage(definition, "s0", contract.firstNpc(),
				QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
			assertPage(definition, "s0", contract.firstNpc(),
				QuestDialogAction.SELECT2_1_1, QuestDialogPage.SELECT2_1_1);
			assertTalk(definition, "s0", "s1", contract.firstNpc(),
				QuestDialogAction.SETPRO2, List.of(),
				List.of(new QuestAction.SetVariable("var0", 1)),
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog()));

			assertPage(definition, "s1", contract.secondNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
			assertPage(definition, "s1", contract.secondNpc(),
				QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
			// 领奖交接写的就是客户端领奖行 2（批次 5 把 reward 从第 2 行推到第 3 行）。
			// The reward hand-over writes the client reward row 2 (batch 5 moved REWARD from row 2 to row 3).
			assertTalk(definition, "s1", "reward", contract.secondNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD, List.of(),
				List.of(new QuestAction.SetVariable("var0", 2)),
				List.of(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));

			QuestTransition reopen = talk(definition, "reward", contract.endNpc(),
				QuestDialogAction.QUEST_SELECT, List.of());
			assertEquals("reward", reopen.targetNode());
			assertTrue(reopen.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
			assertSoleCompletionOwner(definition, contract.endNpc());
		}
	}

	private static void assertStartContract(QuestDefinition definition, int npcId) {
		assertPageTransition(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.QUEST_SELECT, List.of(), QuestDialogPage.SELECT_NONE);
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
		try (InputStream input = ThreeStageReportClientPathTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private record Case(int questId, String race, int startNpc, int firstNpc, int secondNpc,
			int endNpc) {
	}
}
