package com.aionemu.gameserver.questEngine.runtime;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable full-run shadow gate input.
 *
 * <p>Owner coverage remains visible for operations, but the gate is exact:
 * every expected event/path/contract key must be covered and no unexpected key
 * may appear. Repeating one owner or path cannot fill another path.</p>
 */
public record QuestShadowBatchReport(Set<Integer> expectedOwners, Set<Integer> coveredOwners,
			Set<QuestShadowCoverageKey> expectedCoverage, Set<QuestShadowCoverageKey> coveredCoverage,
			List<QuestShadowComparison> comparisons) {
	public QuestShadowBatchReport {
		expectedOwners = Set.copyOf(Objects.requireNonNull(expectedOwners, "expectedOwners"));
		coveredOwners = Set.copyOf(Objects.requireNonNull(coveredOwners, "coveredOwners"));
		expectedCoverage = Set.copyOf(Objects.requireNonNull(expectedCoverage, "expectedCoverage"));
		coveredCoverage = Set.copyOf(Objects.requireNonNull(coveredCoverage, "coveredCoverage"));
		comparisons = List.copyOf(Objects.requireNonNull(comparisons, "comparisons"));
	}

	/** Backward-compatible accessor: the unique-owner gate size, not invocation count. */
	public int expectedInvocations() {
		return expectedOwners.size();
	}

	/** Backward-compatible accessor: unique covered owners, not invocation count. */
	public int actualInvocations() {
		return coveredOwners.size();
	}

	/** Owners that must appear in at least one envelope but have not yet. */
	public Set<Integer> missingOwners() {
		Set<Integer> missing = new LinkedHashSet<>(expectedOwners);
		missing.removeAll(coveredOwners);
		return Collections.unmodifiableSet(missing);
	}

	/** Owners that appeared but were not expected (candidate catalog drift). */
	public Set<Integer> unexpectedOwners() {
		Set<Integer> unexpected = new LinkedHashSet<>(coveredOwners);
		unexpected.removeAll(expectedOwners);
		return Collections.unmodifiableSet(unexpected);
	}

	public Set<QuestShadowCoverageKey> missingCoverage() {
		Set<QuestShadowCoverageKey> missing = new LinkedHashSet<>(expectedCoverage);
		missing.removeAll(coveredCoverage);
		return Collections.unmodifiableSet(missing);
	}

	public Set<QuestShadowCoverageKey> unexpectedCoverage() {
		Set<QuestShadowCoverageKey> unexpected = new LinkedHashSet<>(coveredCoverage);
		unexpected.removeAll(expectedCoverage);
		return Collections.unmodifiableSet(unexpected);
	}

	/** A partial sample is never sufficient for the global owner-switch gate. */
	public boolean complete() {
		return coveredOwners.equals(expectedOwners) && coveredCoverage.equals(expectedCoverage);
	}

	public boolean clean() {
		return complete() && comparisons.stream().allMatch(QuestShadowComparison::clean);
	}

	public Map<QuestShadowDifferenceKind, Integer> differenceCounts() {
		EnumMap<QuestShadowDifferenceKind, Integer> counts = new EnumMap<>(QuestShadowDifferenceKind.class);
		for (QuestShadowComparison comparison : comparisons) {
			for (QuestShadowDifference difference : comparison.differences()) {
				counts.merge(difference.kind(), 1, Integer::sum);
			}
		}
		return Collections.unmodifiableMap(counts);
	}

	/**
	 * Merges consecutive persisted batches for the same gate. Coverage is a set
	 * union while every typed difference remains sticky and deduplicated by event:
	 * a later clean or repeated sample cannot erase or unboundedly duplicate it.
	 */
	public QuestShadowBatchReport merge(QuestShadowBatchReport other) {
		Objects.requireNonNull(other, "other");
		if (!expectedOwners.equals(other.expectedOwners)
				|| !expectedCoverage.equals(other.expectedCoverage)) {
			throw new IllegalArgumentException("shadow reports belong to different owner/coverage gates");
		}
		Set<Integer> mergedOwners = new LinkedHashSet<>(coveredOwners);
		mergedOwners.addAll(other.coveredOwners);
		Set<QuestShadowCoverageKey> mergedCoverage = new LinkedHashSet<>(coveredCoverage);
		mergedCoverage.addAll(other.coveredCoverage);
		Map<String, Set<QuestShadowDifference>> differencesByEvent = new TreeMap<>();
		Comparator<QuestShadowDifference> differenceOrder = Comparator
			.comparingInt(QuestShadowDifference::questId)
			.thenComparing(QuestShadowDifference::kind);
		for (QuestShadowComparison comparison : concat(comparisons, other.comparisons)) {
			if (!comparison.clean()) {
				differencesByEvent.computeIfAbsent(comparison.eventType(), ignored -> new TreeSet<>(differenceOrder))
					.addAll(comparison.differences());
			}
		}
		List<QuestShadowComparison> mergedComparisons = differencesByEvent.entrySet().stream()
			.map(entry -> new QuestShadowComparison(entry.getKey(), List.copyOf(entry.getValue())))
			.toList();
		return new QuestShadowBatchReport(expectedOwners, mergedOwners, expectedCoverage,
			mergedCoverage, mergedComparisons);
	}

	private static List<QuestShadowComparison> concat(List<QuestShadowComparison> left,
			List<QuestShadowComparison> right) {
		List<QuestShadowComparison> result = new java.util.ArrayList<>(left.size() + right.size());
		result.addAll(left);
		result.addAll(right);
		return result;
	}
}
