package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory sink for one shadow run. It keeps immutable observations and
 * exposes comparison only when the caller supplies the authoritative typed
 * event and immutable player snapshots.
 */
public final class QuestLegacyObservationStore implements QuestLegacyObservationSink {
	private final List<QuestLegacyInvocation> observations = new ArrayList<>();

	@Override
	public synchronized void record(QuestLegacyInvocation observation) {
		observations.add(Objects.requireNonNull(observation, "observation"));
	}

	public synchronized List<QuestLegacyInvocation> snapshot() {
		return List.copyOf(observations);
	}

	public synchronized List<QuestShadowDifference> compare(QuestShadowRunner runner,
			QuestLegacyInvocation actual, QuestEvent event, Map<Integer, QuestSnapshot> snapshots) {
		Objects.requireNonNull(runner, "runner");
		Objects.requireNonNull(actual, "actual");
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(snapshots, "snapshots");
		return QuestShadowComparator.compare(runner.inspect(event, snapshots), actual);
	}
}
