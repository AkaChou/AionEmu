package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Runs typed shadow comparisons without invoking a handler or a side effect.
 *
 * <p>One {@link Envelope} is one physical event: the authoritative typed event,
 * the frozen pre-event snapshots of every candidate owner and the aggregated
 * legacy observations of every owner that actually ran inside the event. The
 * candidate result and the legacy result are therefore compared at the same
 * granularity — never a single-owner legacy result against every candidate
 * owner.</p>
 *
 * <p>Completeness is measured by exact event/path/contract coverage. Repeating
 * one owner or one path cannot satisfy another path's gate.</p>
 */
public final class QuestShadowBatchRunner {
	private QuestShadowBatchRunner() {
	}

	public static QuestShadowBatchReport compare(QuestShadowRunner runner, List<Envelope> envelopes,
			Set<Integer> expectedOwners) {
		Objects.requireNonNull(runner, "runner");
		Objects.requireNonNull(envelopes, "envelopes");
		Objects.requireNonNull(expectedOwners, "expectedOwners");
		Set<Integer> coveredOwners = new LinkedHashSet<>();
		Set<QuestShadowCoverageKey> expectedCoverage = runner.expectedCoverage(expectedOwners);
		Set<QuestShadowCoverageKey> coveredCoverage = new LinkedHashSet<>();
		List<QuestShadowComparison> comparisons = new ArrayList<>(envelopes.size());
		for (Envelope envelope : envelopes) {
			QuestShadowRunner.QuestShadowResult candidate = runner.inspect(envelope.event(), envelope.snapshots());
			// 只比较本物理事件实际派发的 owner（快照集即派发集）。候选索引到同 NPC 的兄弟
			// 任务未在本事件派发，保留它们会产生系统性假 ROUTE（talk 直通只派发触发 owner）。
			Set<Integer> dispatched = new LinkedHashSet<>(envelope.snapshots().keySet());
			dispatched.addAll(envelope.observation().owners().keySet());
			List<QuestShadowDifference> differences = QuestShadowComparator.compare(
				candidate.scopedTo(dispatched), envelope.observation());
			coveredOwners.addAll(envelope.observation().owners().keySet());
			for (QuestLegacyInvocation invocation : envelope.invocations()) {
				QuestShadowObservation.Owner actualOwner = invocation.observation().owners().get(invocation.questId());
				if (actualOwner == null || !actualOwner.conditionMatched()) {
					continue;
				}
				candidate.owners().stream()
					.filter(owner -> owner.questId() == invocation.questId() && owner.plan().isPresent())
					.map(owner -> owner.observedCoverage(invocation.eventType(), invocation.contract()))
					.forEach(coveredCoverage::add);
			}
			comparisons.add(new QuestShadowComparison(candidate.event().type(), differences));
		}
		return new QuestShadowBatchReport(expectedOwners, coveredOwners, expectedCoverage, coveredCoverage, comparisons);
	}

	/** One physical event as one atomic shadow input. */
	public record Envelope(QuestEvent event, Map<Integer, QuestSnapshot> snapshots,
			QuestShadowObservation observation, List<QuestLegacyInvocation> invocations) {
		public Envelope {
			event = Objects.requireNonNull(event, "event");
			snapshots = Map.copyOf(Objects.requireNonNull(snapshots, "snapshots"));
			observation = Objects.requireNonNull(observation, "observation");
			invocations = List.copyOf(Objects.requireNonNull(invocations, "invocations"));
		}
	}
}
