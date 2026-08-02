package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;

/** Fail-closed global gate; readiness never changes production ownership by itself. */
public final class QuestOwnerSwitchGate {
	private QuestOwnerSwitchGate() {
	}

	public static void requireReady(Inputs inputs) {
		Objects.requireNonNull(inputs, "inputs");
		if (inputs.capabilityCoveragePercent() != 100
				|| inputs.unmapped() != 0
				|| inputs.unclassifiedCalls() != 0
				|| inputs.taskSpecialCases() != 0
				|| !inputs.candidateComplete()
				|| !inputs.fullDryRunComplete()
				|| !inputs.shadowClean()
				|| inputs.conversionRequiresNewRuntime() != 0
				|| inputs.ownershipOverlap() != 0
				|| inputs.productionOwnerSwitch() != 0) {
			throw new QuestCompilationException("PRODUCTION_OWNER_SWITCH_BLOCKED",
					"quest owner switch requires every global gate to be closed");
		}
	}

	public record Inputs(int capabilityCoveragePercent, int unmapped, int unclassifiedCalls,
			int taskSpecialCases, boolean candidateComplete, boolean fullDryRunComplete,
			boolean shadowClean, int conversionRequiresNewRuntime, int ownershipOverlap,
			int productionOwnerSwitch) {
		public Inputs {
			if (capabilityCoveragePercent < 0 || capabilityCoveragePercent > 100
					|| unmapped < 0 || unclassifiedCalls < 0 || taskSpecialCases < 0
					|| conversionRequiresNewRuntime < 0 || ownershipOverlap < 0
					|| productionOwnerSwitch < 0 || productionOwnerSwitch > 1) {
				throw new IllegalArgumentException("global gate counters are out of range");
			}
		}
	}
}
