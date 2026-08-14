package com.aionemu.gameserver.questEngine.runtime;

import java.util.Objects;

/** 权威任务开始检查的不可变事件前结果。 / Immutable pre-event result of the authoritative quest-start checks. */
public record QuestStartEligibility(boolean eligible, String reason) {
	public QuestStartEligibility {
		reason = Objects.requireNonNull(reason, "reason");
		if (eligible && !reason.isEmpty()) {
			throw new IllegalArgumentException("eligible start must not carry a rejection reason");
		}
		if (!eligible && reason.isBlank()) {
			throw new IllegalArgumentException("ineligible start requires a rejection reason");
		}
	}

	public static QuestStartEligibility allowed() {
		return new QuestStartEligibility(true, "");
	}

	public static QuestStartEligibility rejected(String reason) {
		return new QuestStartEligibility(false, reason);
	}
}
