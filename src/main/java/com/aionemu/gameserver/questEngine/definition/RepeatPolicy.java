package com.aionemu.gameserver.questEngine.definition;

/** Repeat metadata kept separate from execution transitions. */
public record RepeatPolicy(int maxRepeatCount, long cooldownSeconds, boolean daily, boolean weekly) {
	public RepeatPolicy {
		if (maxRepeatCount < 0 || maxRepeatCount == 0) {
			throw new IllegalArgumentException("maxRepeatCount must be positive or 255 for unlimited");
		}
		if (cooldownSeconds < 0) {
			throw new IllegalArgumentException("cooldownSeconds must be non-negative");
		}
		if (daily && weekly) {
			throw new IllegalArgumentException("a repeat policy cannot be both daily and weekly");
		}
	}

	public static RepeatPolicy once() {
		return new RepeatPolicy(1, 0, false, false);
	}
}
