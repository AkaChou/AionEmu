package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 3502 的深渊之门启动、无序守卫击杀和独占领奖合同。
 * Verifies quest 3502's Abyss Gate activation, unordered guardian kills, and exclusive reward contract.
 */
class Quest3502RetailFlowAlignmentTest {
	private static final int START_AND_REWARD_NPC = 204656;
	private static final int GATE = 730192;
	private static final int FIRST_TARGET = 214894;
	private static final int FINAL_TARGET = 214904;
	private static final Map<Integer, String> GUARDIAN_FLAGS = Map.of(
		214895, "var1",
		214896, "var2",
		214897, "var3");

	@Test
	void preservesGateActivationIndependentGuardianFlagsAndFinalRewardOwner() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();
		assertNodes(definition);
		assertDialogContract(definition);
		assertKillContract(definition);
		assertCompletionContract(definition);
		assertRuntimeOrder(compiled);
	}

	private static void assertNodes(QuestDefinition definition) {
		Map<String, Integer> zeroes = Map.of("var0", 0, "var1", 0, "var2", 0, "var3", 0);
		assertNode(definition, "unaccepted", QuestStatus.NONE, zeroes);
		assertNode(definition, "started", QuestStatus.START, zeroes);
		assertNode(definition, "gate", QuestStatus.START,
			Map.of("var0", 1, "var1", 0, "var2", 0, "var3", 0));
		assertNode(definition, "hunt", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "reward", QuestStatus.REWARD,
			Map.of("var0", 2, "var1", 1, "var2", 1, "var3", 1));
		assertNode(definition, "complete", QuestStatus.COMPLETE, zeroes);
	}

	private static void assertDialogContract(QuestDefinition definition) {
		assertPage(route(definition, "unaccepted", START_AND_REWARD_NPC, QuestDialogAction.QUEST_SELECT),
			"unaccepted", List.of(), QuestDialogPage.SELECT_NONE);
		assertTrue(routes(definition, "unaccepted", GATE).isEmpty());

		QuestTransition gatePage = route(definition, "started", GATE, QuestDialogAction.QUEST_SELECT);
		assertPage(gatePage, "started", List.of(new QuestCondition.QuestVariableIs("var0", 0)),
			QuestDialogPage.SELECT1);

		QuestTransition activate = route(definition, "started", GATE, QuestDialogAction.SETPRO1);
		assertEquals("gate", activate.targetNode());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), activate.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), activate.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), activate.afterCommit());
		assertNull(activate.priority());

		QuestTransition report = route(definition, "reward", START_AND_REWARD_NPC,
			QuestDialogAction.USE_OBJECT);
		assertPage(report, "reward", List.of(), QuestDialogPage.DEFAULT_SUCCESS);
		QuestTransition preview = route(definition, "reward", START_AND_REWARD_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertPage(preview, "reward", List.of(), QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);

		assertTrue(routes(definition, "reward", GATE).isEmpty());
		assertFalse(routes(definition, "started", START_AND_REWARD_NPC).stream()
			.anyMatch(transition -> dialogId(transition) == QuestDialogAction.QUEST_SELECT.id()));
	}

	private static void assertKillContract(QuestDefinition definition) {
		QuestTransition first = transition(definition, "gate", new QuestEvent.KillNpc(FIRST_TARGET));
		assertEquals("hunt", first.targetNode());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), first.conditions());
		assertEquals(List.of(), first.actions());
		assertPacketOnly(first);

		for (Map.Entry<Integer, String> guardian : GUARDIAN_FLAGS.entrySet()) {
			QuestTransition kill = transition(definition, "hunt", new QuestEvent.KillNpc(guardian.getKey()));
			assertEquals("hunt", kill.targetNode());
			assertEquals(List.of(new QuestCondition.QuestVariableIs(guardian.getValue(), 0)),
				kill.conditions());
			assertEquals(List.of(new QuestAction.SetVariable(guardian.getValue(), 1)), kill.actions());
			assertPacketOnly(kill);
		}

		QuestTransition finish = transition(definition, "hunt", new QuestEvent.KillNpc(FINAL_TARGET));
		assertEquals("reward", finish.targetNode());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 2),
			new QuestCondition.QuestVariableIs("var1", 1),
			new QuestCondition.QuestVariableIs("var2", 1),
			new QuestCondition.QuestVariableIs("var3", 1)), finish.conditions());
		assertEquals(List.of(), finish.actions());
		assertPacketOnly(finish);
	}

	private static void assertCompletionContract(QuestDefinition definition) {
		List<QuestTransition> completions = routes(definition, "reward", START_AND_REWARD_NPC).stream()
			.filter(transition -> dialogId(transition) >= QuestDialogAction.SELECTED_QUEST_REWARD1.id())
			.filter(transition -> dialogId(transition) <= QuestDialogAction.SELECTED_QUEST_REWARD13.id())
			.toList();
		assertEquals(13, completions.size());
		for (int index = 0; index < completions.size(); index++) {
			QuestTransition completion = route(definition, "reward", START_AND_REWARD_NPC,
				QuestDialogAction.fromId(QuestDialogAction.SELECTED_QUEST_REWARD1.id() + index));
			assertEquals("complete", completion.targetNode());
			assertEquals(List.of(), completion.conditions());
			assertEquals(expectedCompletionActions(definition, index), completion.actions());
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit());
			assertNull(completion.priority());
		}
		assertEquals(32, definition.transitions().size());
	}

	private static List<QuestAction> expectedCompletionActions(QuestDefinition definition, int choiceIndex) {
		List<QuestAction> actions = new ArrayList<>();
		for (int rewardIndex = 0; rewardIndex < 6; rewardIndex++) {
			actions.add(rewardAction(definition.metadata().rewards().get(rewardIndex)));
		}
		actions.add(rewardAction(definition.metadata().rewards().get(6 + choiceIndex)));
		actions.add(new QuestAction.CompleteQuest(0));
		return List.copyOf(actions);
	}

	private static QuestAction rewardAction(QuestReward reward) {
		QuestRewardKind kind = QuestRewardKind.fromWire(reward.kind());
		QuestRewardKind actionKind = kind == QuestRewardKind.SELECTABLE_ITEM ? QuestRewardKind.ITEM : kind;
		QuestRewardAmountMode amountMode = switch (actionKind) {
			case GOLD, KINAH, EXP, AP, GP -> QuestRewardAmountMode.QUEST_BASE;
			default -> QuestRewardAmountMode.EXACT;
		};
		return new QuestAction.GrantReward(actionKind.name(), reward.id(), reward.amount(), amountMode);
	}

	private static void assertRuntimeOrder(CompiledQuestDefinition definition) {
		QuestSnapshot snapshot = snapshot(definition, QuestStatus.START,
			Map.of("var0", 0, "var1", 0, "var2", 0, "var3", 0));
		snapshot = apply(definition, snapshot,
			route(definition.definition(), "started", GATE, QuestDialogAction.SETPRO1));
		assertVariables(definition, snapshot, Map.of("var0", 1, "var1", 0, "var2", 0, "var3", 0));

		snapshot = apply(definition, snapshot,
			transition(definition.definition(), "gate", new QuestEvent.KillNpc(FIRST_TARGET)));
		assertVariables(definition, snapshot, Map.of("var0", 2, "var1", 0, "var2", 0, "var3", 0));

		QuestTransition finalKill = transition(definition.definition(), "hunt",
			new QuestEvent.KillNpc(FINAL_TARGET));
		assertTrue(QuestMutationPlanner.plan(definition, snapshot, finalKill.event(), finalKill).isEmpty());

		for (int npcId : List.of(214897, 214895, 214896)) {
			QuestTransition guardian = transition(definition.definition(), "hunt", new QuestEvent.KillNpc(npcId));
			snapshot = apply(definition, snapshot, guardian);
			assertTrue(QuestMutationPlanner.plan(definition, snapshot, guardian.event(), guardian).isEmpty());
		}
		assertVariables(definition, snapshot, Map.of("var0", 2, "var1", 1, "var2", 1, "var3", 1));

		snapshot = apply(definition, snapshot, finalKill);
		assertEquals(QuestStatus.REWARD, snapshot.status());
		assertVariables(definition, snapshot, Map.of("var0", 2, "var1", 1, "var2", 1, "var3", 1));
	}

	private static QuestSnapshot apply(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestTransition transition) {
		QuestMutationPlan plan = QuestMutationPlanner.plan(definition, snapshot, transition.event(), transition)
			.orElseThrow();
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), Map.of());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables) {
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(variables), Map.of());
	}

	private static void assertVariables(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			Map<String, Integer> expected) {
		assertEquals(expected, definition.definition().progressLayout().unpack(snapshot.packedVariables()));
	}

	private static void assertPacketOnly(QuestTransition transition) {
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition.afterCommit());
		assertNull(transition.priority());
	}

	private static void assertPage(QuestTransition transition, String target,
			List<QuestCondition> conditions, QuestDialogPage page) {
		assertEquals(target, transition.targetNode());
		assertEquals(conditions, transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
		assertNull(transition.priority());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, matches.size(), source + " " + event);
		return matches.getFirst();
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> matches = routes(definition, source, npcId).stream()
			.filter(candidate -> dialogId(candidate) == action.id())
			.toList();
		assertEquals(1, matches.size(), source + " " + npcId + " " + action);
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static int dialogId(QuestTransition transition) {
		return ((QuestEvent.TalkToNpc) transition.event()).dialogId();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/3502.xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
