package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证共享 START 节点不会把实时击杀计数锁死或重置，并锁定最后一次击杀和报告的生产流。
 * Verifies that shared START nodes neither lock nor reset live kill counters and preserves final-kill/report flows.
 */
class QuestCounterSourceProjectionProductionFlowTest {
	private static final Set<Integer> LIBRARIANS = Set.of(
		220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330);
	private static final Set<Integer> SUB_BOSSES = Set.of(857450, 857452, 857454, 857456, 857458, 857459);
	private static final Set<Integer> CAPTAINS = Set.of(219256, 219259, 219267, 219261, 219271);
	private static final Set<Integer> ADJUTANTS = Set.of(219257, 219258, 219268, 219260, 219262);
	private static final Set<Integer> GI_GUARDIANS = Set.of(219286, 243852);

	@Test
	void quest26802CompletesOnTheLastKillInEitherCounterOrder() throws Exception {
		CompiledQuestDefinition definition = load(26802);
		assertEquals(Map.of(), node(definition, "started").projection().variables());

		assert26802Order(definition, true);
		assert26802Order(definition, false);
	}

	@Test
	void quests30603And30613PreserveFourIndependentRetailCounters() throws Exception {
		assertFourCounterMonsterHunt(load(30603), 800325);
		assertFourCounterMonsterHunt(load(30613), 800327);
	}

	private static void assert26802Order(CompiledQuestDefinition definition, boolean librariansFirst)
			throws Exception {
		QuestTransition continuing = killRoute(definition, LIBRARIANS, 2);
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(continuing);
			if (librariansFirst) {
				dispatchKills(runtime, 220306, 30);
				assertEquals(QuestStatus.START, runtime.state().status());
				dispatchKills(runtime, 857450, 2);
			} else {
				dispatchKills(runtime, 857450, 2);
				assertEquals(QuestStatus.START, runtime.state().status());
				dispatchKills(runtime, 220306, 30);
			}
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(Map.of("var0", 1, "var1", 30, "var2", 2), variables(definition, runtime));
		}
	}

	private static void assertFourCounterMonsterHunt(CompiledQuestDefinition definition, int reportNpcId)
			throws Exception {
		assertEquals(Map.of(), node(definition, "started").projection().variables());
		assertEquals(new NodeProjection(QuestStatus.REWARD,
			Map.of("var0", 9, "var1", 9, "var2", 1, "var3", 1)),
			node(definition, "reward").projection());
		assertCounter(definition, CAPTAINS, "var0", 9);
		assertCounter(definition, ADJUTANTS, "var1", 9);
		assertCounter(definition, Set.of(219255), "var2", 1);
		assertCounter(definition, GI_GUARDIANS, "var3", 1);

		QuestTransition report = definition.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals("started"))
			.filter(candidate -> candidate.targetNode().equals("reward"))
			.filter(candidate -> candidate.event().equals(
				new QuestEvent.TalkToNpc(reportNpcId, QuestDialogAction.SELECT_QUEST_REWARD.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(
			new QuestCondition.VariableAtLeast("var0", 9),
			new QuestCondition.VariableAtLeast("var1", 9),
			new QuestCondition.VariableAtLeast("var2", 1),
			new QuestCondition.VariableAtLeast("var3", 1)), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		QuestTransition firstCounter = killRoute(definition, CAPTAINS, 1);
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(firstCounter);
			dispatchKills(runtime, 243852, 1);
			dispatchKills(runtime, 219255, 1);
			dispatchKills(runtime, 219257, 9);
			dispatchKills(runtime, 219256, 9);
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 9, "var1", 9, "var2", 1, "var3", 1),
				variables(definition, runtime));
			assertFalse(runtime.dispatchWorld(new QuestEvent.KillNpc(219256)).handled());
			assertEquals(Map.of("var0", 9, "var1", 9, "var2", 1, "var3", 1),
				variables(definition, runtime));
		}

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(report);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertTrue(outcome.handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(Map.of("var0", 9, "var1", 9, "var2", 1, "var3", 1),
				variables(definition, runtime));
		}
	}

	private static void assertCounter(CompiledQuestDefinition definition, Set<Integer> npcIds, String field,
			int required) {
		QuestTransition transition = killRoute(definition, npcIds, 1);
		assertEquals("started", transition.sourceNode());
		assertEquals("started", transition.targetNode());
		assertEquals(List.of(new QuestCondition.VariableBelow(field, required)), transition.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), transition.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition.afterCommit());
	}

	private static QuestTransition killRoute(CompiledQuestDefinition definition, Set<Integer> npcIds,
			int priority) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(new QuestEvent.KillNpcSet(npcIds)))
			.filter(candidate -> Objects.equals(candidate.priority(), priority))
			.findFirst().orElseThrow();
	}

	private static void dispatchKills(QuestE2eRuntime runtime, int npcId, int count) {
		for (int index = 0; index < count; index++) {
			assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(npcId)).handled(),
				"kill " + npcId + " was not handled at index " + index);
		}
	}

	private static Map<String, Integer> variables(CompiledQuestDefinition definition, QuestE2eRuntime runtime) {
		return definition.definition().progressLayout().unpack(runtime.state().packedVariables());
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				QuestCounterSourceProjectionProductionFlowTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
