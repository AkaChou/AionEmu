package com.aionemu.gameserver.questEngine.runtime;

/**
 * Typed boundary for observations captured around a legacy owner invocation.
 *
 * <p>The sink is deliberately passive: it receives an immutable observation
 * after the legacy handler has returned and must not be used to route or
 * mutate quest state.</p>
 */
@FunctionalInterface
public interface QuestLegacyObservationSink {
	QuestLegacyObservationSink NOOP = ignored -> {
	};

	void record(QuestLegacyInvocation observation);
}
