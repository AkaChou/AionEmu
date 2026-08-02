package com.aionemu.gameserver.questEngine.runtime;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
}
