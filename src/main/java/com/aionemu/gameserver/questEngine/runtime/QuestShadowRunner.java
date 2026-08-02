package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Side-effect-free candidate shadow entry point.
 * It only indexes routes, evaluates supplied snapshots, and creates mutation plans.
 */
public final class QuestShadowRunner {
	private final QuestCatalog catalog;
	private final QuestEventIndex index;

	public QuestShadowRunner(QuestCatalog catalog) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.index = new QuestEventIndex(catalog);
	}

	public QuestShadowResult inspect(QuestEvent event, Map<Integer, QuestSnapshot> snapshots) {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(snapshots, "snapshots");
		List<QuestShadowOwner> owners = new ArrayList<>();
		for (QuestEventIndex.Route route : index.routesFor(event)) {
			CompiledQuestDefinition definition = catalog.find(route.questId()).orElseThrow();
			QuestSnapshot snapshot = snapshots.get(route.questId());
			if (snapshot != null && snapshot.questId() != route.questId()) {
				throw new IllegalArgumentException("snapshot quest id does not match route owner");
			}
			Optional<QuestMutationPlan> plan = snapshot == null
				? Optional.empty()
				: QuestMutationPlanner.plan(definition, snapshot, event, route.transition());
			owners.add(new QuestShadowOwner(definition, route.transition(), plan));
		}
		return new QuestShadowResult(event, selectPriorityWinners(owners));
	}

	private static List<QuestShadowOwner> selectPriorityWinners(List<QuestShadowOwner> owners) {
		Map<Integer, List<QuestShadowOwner>> byOwner = owners.stream()
			.collect(java.util.stream.Collectors.groupingBy(QuestShadowOwner::questId,
				java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));
		List<QuestShadowOwner> resolved = new ArrayList<>(owners.size());
		for (List<QuestShadowOwner> routes : byOwner.values()) {
			List<QuestShadowOwner> matched = routes.stream().filter(owner -> owner.plan().isPresent()).toList();
			QuestShadowOwner winner = null;
			if (matched.size() == 1) {
				winner = matched.get(0);
			} else if (matched.size() > 1) {
				if (matched.stream().anyMatch(owner -> owner.transition().priority() == null)) {
					throw new IllegalStateException("multiple matching shadow routes require explicit priorities");
				}
				winner = matched.stream().min(java.util.Comparator.comparingInt(
					owner -> owner.transition().priority())).orElseThrow();
			}
			for (QuestShadowOwner route : routes) {
				resolved.add(route == winner || route.plan().isEmpty() ? route
					: new QuestShadowOwner(route.definition(), route.transition(), Optional.empty()));
			}
		}
		return List.copyOf(resolved);
	}

	Set<QuestShadowCoverageKey> expectedCoverage(Set<Integer> expectedOwners) {
		Set<QuestShadowCoverageKey> coverage = new LinkedHashSet<>();
		for (CompiledQuestDefinition definition : catalog.all().stream()
				.sorted(java.util.Comparator.comparingInt(CompiledQuestDefinition::id)).toList()) {
			if (!expectedOwners.contains(definition.id())) {
				continue;
			}
			for (var transition : definition.definition().transitions()) {
				if (!coverage.add(QuestShadowCoverageKey.expected(definition, transition))) {
					throw new IllegalStateException("duplicate shadow coverage key for quest " + definition.id());
				}
			}
		}
		return Set.copyOf(coverage);
	}

	public record QuestShadowResult(QuestEvent event, List<QuestShadowOwner> owners) {
		public QuestShadowResult {
			event = Objects.requireNonNull(event, "event");
			owners = List.copyOf(Objects.requireNonNull(owners, "owners"));
		}

		public boolean hasCandidatePlan() {
			return owners.stream().anyMatch(owner -> owner.plan().isPresent());
		}

		/**
		 * Restricts the candidate result to the owners actually part of one
		 * physical event. A candidate indexes every route on the event's NPC
		 * (sibling quests included), but only the dispatched owners are
		 * comparable against the legacy observation of that event; the others
		 * would otherwise produce systematic false ROUTE differences.
		 */
		public QuestShadowResult scopedTo(Set<Integer> questIds) {
			if (questIds == null) {
				throw new IllegalArgumentException("questIds must not be null");
			}
			List<QuestShadowOwner> kept = owners.stream()
					.filter(owner -> questIds.contains(owner.questId()))
					.toList();
			return new QuestShadowResult(event, kept);
		}
	}

	public record QuestShadowOwner(CompiledQuestDefinition definition,
			com.aionemu.gameserver.questEngine.definition.QuestTransition transition,
			Optional<QuestMutationPlan> plan) {
		public QuestShadowOwner {
			definition = Objects.requireNonNull(definition, "definition");
			transition = Objects.requireNonNull(transition, "transition");
			plan = Objects.requireNonNull(plan, "plan");
		}

		public int questId() {
			return definition.id();
		}

		QuestShadowCoverageKey observedCoverage(String eventType, QuestDispatchContract contract) {
			return QuestShadowCoverageKey.observed(definition, transition, eventType, contract);
		}
	}
}
