package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 审计客户端可见的 SETPRO 直接领奖路由，避免迁移遗漏中间阶段或材料条件。
 * Audits client-visible SETPRO routes that claim rewards directly, preventing migrated definitions from
 * skipping an intermediate stage or its material guard.
 */
final class QuestPrematureRewardRouteAudit {
	static final String SOURCE_HAS_PROGRESS_ROUTE = "SOURCE_HAS_PROGRESS_ROUTE";
	static final String MULTIPLE_UNGUARDED_REWARD_CHOICES = "MULTIPLE_UNGUARDED_REWARD_CHOICES";

	private QuestPrematureRewardRouteAudit() {
	}

	static List<Violation> audit(QuestCatalog catalog,
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests) {
		Objects.requireNonNull(catalog, "catalog");
		Objects.requireNonNull(clientQuests, "clientQuests");
		List<Violation> result = new ArrayList<>();
		clientQuests.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
			catalog.findExecutable(entry.getKey()).ifPresent(compiled ->
				result.addAll(audit(compiled.definition(), entry.getValue()))));
		return sorted(result);
	}

	static List<Violation> audit(QuestDefinition definition, QuestDialogOrderAudit.ClientQuest client) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(client, "client");
		if ("EVENT".equalsIgnoreCase(definition.metadata().category())) {
			return List.of();
		}

		List<Candidate> candidates = definition.transitions().stream()
			.map(transition -> candidate(definition, client, transition))
			.flatMap(java.util.Optional::stream)
			.toList();
		List<Violation> result = new ArrayList<>();
		for (Candidate candidate : candidates) {
			Set<String> reasons = new LinkedHashSet<>();
			List<String> evidence = new ArrayList<>();

			List<QuestTransition> progressRoutes = definition.transitions().stream()
				.filter(transition -> Objects.equals(transition.sourceNode(), candidate.sourceNode()))
				.filter(transition -> !transition.targetNode().equals(candidate.sourceNode()))
				.filter(transition -> node(definition, transition.targetNode()).projection().status() == QuestStatus.START)
				.toList();
			if (!progressRoutes.isEmpty()) {
				reasons.add(SOURCE_HAS_PROGRESS_ROUTE);
				evidence.add("progress-targets=" + progressRoutes.stream()
					.map(QuestTransition::targetNode).distinct().sorted().toList());
			}

			List<QuestDialogOrderAudit.ClientPage> choicePages = choicePages(candidate, candidates, client);
			if (!choicePages.isEmpty()) {
				reasons.add(MULTIPLE_UNGUARDED_REWARD_CHOICES);
				evidence.add("client-pages=" + choicePages.stream()
					.map(QuestDialogOrderAudit.ClientPage::pageId).sorted().toList());
			}

			if (!reasons.isEmpty()) {
				result.add(new Violation(definition.id(), candidate.sourceNode(), QuestStatus.START,
					candidate.npcId(), candidate.dialogId(), String.join("+", reasons),
					String.join("; ", evidence)));
			}
		}
		return sorted(result);
	}

	private static java.util.Optional<Candidate> candidate(QuestDefinition definition,
		QuestDialogOrderAudit.ClientQuest client, QuestTransition transition) {
		if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)
			|| transition.sourceNode() == null
			|| transition.conditions().size() != 0
			|| transition.actions().size() != 0) {
			return java.util.Optional.empty();
		}
		QuestNode source = node(definition, transition.sourceNode());
		QuestNode target = node(definition, transition.targetNode());
		if (source.projection().status() != QuestStatus.START || target.projection().status() != QuestStatus.REWARD) {
			return java.util.Optional.empty();
		}
		Integer dialogId = setproActionId(talk);
		if (dialogId == null || client.pages().values().stream()
			.noneMatch(page -> page.actions().containsKey(dialogId))) {
			return java.util.Optional.empty();
		}
		return java.util.Optional.of(new Candidate(transition, talk.npcId(), dialogId, transition.sourceNode()));
	}

	private static Integer setproActionId(QuestEvent.TalkToNpc talk) {
		if (talk.dialogId() == null) {
			return null;
		}
		try {
			QuestDialogAction action = QuestDialogAction.fromId(talk.dialogId());
			return action.name().startsWith("SETPRO") ? action.id() : null;
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static List<QuestDialogOrderAudit.ClientPage> choicePages(Candidate candidate,
		List<Candidate> candidates, QuestDialogOrderAudit.ClientQuest client) {
		List<Candidate> siblingChoices = candidates.stream()
			.filter(sibling -> sibling.sourceNode().equals(candidate.sourceNode()))
			.filter(sibling -> sibling.npcId() == candidate.npcId())
			.filter(sibling -> sibling.transition().targetNode().equals(candidate.transition().targetNode()))
			.toList();
		return client.pages().values().stream()
			.filter(page -> siblingChoices.stream()
				.filter(sibling -> page.actions().containsKey(sibling.dialogId()))
				.count() >= 2)
			.sorted(Comparator.comparingInt(QuestDialogOrderAudit.ClientPage::pageId))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow(() -> new IllegalArgumentException("unknown quest node " + label));
	}

	private static List<Violation> sorted(List<Violation> violations) {
		return violations.stream().distinct()
			.sorted(Comparator.comparingInt(Violation::questId)
				.thenComparing(Violation::sourceNode)
				.thenComparingInt(Violation::npcId)
				.thenComparingInt(Violation::dialogId)
				.thenComparing(Violation::reason))
			.toList();
	}

	record Violation(int questId, String sourceNode, QuestStatus sourceStatus, int npcId,
		int dialogId, String reason, String evidence) {
	}

	private record Candidate(QuestTransition transition, int npcId, int dialogId, String sourceNode) {
	}
}
