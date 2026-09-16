package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestPrematureRewardRouteAuditTest {
	@Test
	void allowsGuardedDirectRewardRoute() {
		QuestDefinition definition = definition(List.of(
				node("unaccepted", QuestStatus.NONE), node("started", QuestStatus.START),
				node("reward", QuestStatus.REWARD)), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(900001, QuestDialogAction.SETPRO1.id()),
				List.of(new QuestCondition.HasItem(182400001, 1)),
				List.of(new QuestAction.RemoveItem(182400001, 1)), "reward", List.of(), 0, "started")));

		assertEquals(List.of(), QuestPrematureRewardRouteAudit.audit(definition,
			clientQuest(900001, QuestDialogAction.SETPRO1.id())));
	}

	@Test
	void reportsAnUnconditionalRewardRouteWhenTheSourceHasANextStage() {
		QuestDefinition definition = definition(List.of(
				node("unaccepted", QuestStatus.NONE), node("started", QuestStatus.START),
				node("stage", QuestStatus.START), node("reward", QuestStatus.REWARD)), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(900002, QuestDialogAction.SETPRO1.id()),
				List.of(), List.of(), "reward", List.of(), 0, "started"),
			new QuestTransition(new QuestEvent.TalkToNpc(900002, QuestDialogAction.SETPRO2.id()),
				List.of(), List.of(), "stage", List.of(), 0, "started")));

		List<QuestPrematureRewardRouteAudit.Violation> violations = QuestPrematureRewardRouteAudit.audit(definition,
			clientQuest(900002, QuestDialogAction.SETPRO1.id()));

		assertEquals(1, violations.size());
		assertEquals(QuestPrematureRewardRouteAudit.SOURCE_HAS_PROGRESS_ROUTE, violations.getFirst().reason());
		// 审计对外证据固定带 progress-targets= 前缀（生产门禁按同一格式输出）。
		// The audit always prefixes this evidence with progress-targets= (the production gate prints the same shape).
		assertEquals("progress-targets=[stage]", violations.getFirst().evidence());
	}

	@Test
	void reportsEveryUnconditionalChoiceOnTheSameClientPage() {
		// 同一 NPC、同一来源、指向同一奖励节点的多个无条件选择才算“重复领奖选择”；
		// 指向不同奖励节点的分支由 allowsDistinctRewardBranchesOnTheSameClientPage 放行。
		// Only multiple unconditional choices from the same source and NPC into the same reward node
		// count as duplicate reward choices; distinct reward branches are allowed by the sibling test.
		QuestDefinition definition = definition(List.of(
				node("unaccepted", QuestStatus.NONE), node("started", QuestStatus.START),
				node("reward", QuestStatus.REWARD)), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(900003, QuestDialogAction.SETPRO1.id()),
				List.of(), List.of(), "reward", List.of(), 0, "started"),
			new QuestTransition(new QuestEvent.TalkToNpc(900003, QuestDialogAction.SETPRO2.id()),
				List.of(), List.of(), "reward", List.of(), 0, "started")));

		List<QuestPrematureRewardRouteAudit.Violation> violations = QuestPrematureRewardRouteAudit.audit(definition,
			clientQuest(900003, QuestDialogAction.SETPRO1.id(), QuestDialogAction.SETPRO2.id()));

		assertEquals(2, violations.size());
		assertEquals(List.of(QuestDialogAction.SETPRO1.id(), QuestDialogAction.SETPRO2.id()),
			violations.stream().map(QuestPrematureRewardRouteAudit.Violation::dialogId).toList());
		assertEquals(List.of(QuestPrematureRewardRouteAudit.MULTIPLE_UNGUARDED_REWARD_CHOICES,
			QuestPrematureRewardRouteAudit.MULTIPLE_UNGUARDED_REWARD_CHOICES),
			violations.stream().map(QuestPrematureRewardRouteAudit.Violation::reason).toList());
	}

	@Test
	void allowsDistinctRewardBranchesOnTheSameClientPage() {
		QuestDefinition definition = definition(List.of(
				node("unaccepted", QuestStatus.NONE), node("started", QuestStatus.START),
				node("reward0", QuestStatus.REWARD, 1), node("reward1", QuestStatus.REWARD, 2)), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(900004, QuestDialogAction.SETPRO1.id()),
				List.of(), List.of(), "reward0", List.of(), 0, "started"),
			new QuestTransition(new QuestEvent.TalkToNpc(900004, QuestDialogAction.SETPRO2.id()),
				List.of(), List.of(), "reward1", List.of(), 0, "started")));

		assertEquals(List.of(), QuestPrematureRewardRouteAudit.audit(definition,
			clientQuest(900004, QuestDialogAction.SETPRO1.id(), QuestDialogAction.SETPRO2.id())));
	}

	private static QuestDefinition definition(List<QuestNode> nodes, List<QuestTransition> transitions) {
		return new QuestDefinition(900000, 1, QuestMetadata.minimal("premature-reward-audit", 900000, "QUEST"),
			ProgressLayout.empty(), nodes, transitions);
	}

	private static QuestNode node(String label, QuestStatus status) {
		return node(label, status, 0);
	}

	private static QuestNode node(String label, QuestStatus status, int var0) {
		return new QuestNode(label, new NodeProjection(status, var0 == 0 ? Map.of() : Map.of("var0", var0)));
	}

	private static QuestDialogOrderAudit.ClientQuest clientQuest(int questId, int... actionIds) {
		Map<Integer, QuestDialogOrderAudit.ClientAction> actions = new LinkedHashMap<>();
		for (int actionId : actionIds) {
			actions.put(actionId, new QuestDialogOrderAudit.ClientAction(actionId, "synthetic page"));
		}
		return new QuestDialogOrderAudit.ClientQuest("QUEST_Q" + questId + ".html", Map.of(1352,
			new QuestDialogOrderAudit.ClientPage(1352, "select2", 1, Map.copyOf(actions), Map.of(),
				"synthetic page")));
	}
}
