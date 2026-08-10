package com.aionemu.gameserver.questEngine.definition;

/** Repeat metadata kept separate from execution transitions. */
public record RepeatPolicy(int maxRepeatCount, int rewardRepeatCount, long cooldownSeconds,
		boolean daily, boolean weekly) {
	public RepeatPolicy(int maxRepeatCount, long cooldownSeconds, boolean daily, boolean weekly) {
		this(maxRepeatCount, maxRepeatCount < 255 ? maxRepeatCount : 0,
			cooldownSeconds, daily, weekly);
	}

	public RepeatPolicy {
		if (maxRepeatCount < 0 || maxRepeatCount == 0) {
			throw new IllegalArgumentException("maxRepeatCount must be positive or 255 for unlimited");
		}
		if (rewardRepeatCount < 0 || rewardRepeatCount > maxRepeatCount) {
			throw new IllegalArgumentException("rewardRepeatCount must be between zero and maxRepeatCount");
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
