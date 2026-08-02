package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;

/** Complete timer identity and lifecycle policy carried by the immutable IR. */
public record QuestTimerPolicy(Identity identity, Persistence persistence,
		OverwritePolicy overwritePolicy, Delivery delivery) {
	public static final String VISIBLE_TIMER_ID = "visible";
	public static final String INVISIBLE_TIMER_ID = "invisible";

	public QuestTimerPolicy {
		identity = Objects.requireNonNull(identity, "identity");
		persistence = Objects.requireNonNull(persistence, "persistence");
		overwritePolicy = Objects.requireNonNull(overwritePolicy, "overwritePolicy");
		delivery = Objects.requireNonNull(delivery, "delivery");
	}

	public static QuestTimerPolicy visible() {
		return session(VISIBLE_TIMER_ID, OverwritePolicy.REPLACE);
	}

	public static QuestTimerPolicy invisible() {
		return session(INVISIBLE_TIMER_ID, OverwritePolicy.REPLACE);
	}

	public static QuestTimerPolicy session(String timerId, OverwritePolicy overwritePolicy) {
		return new QuestTimerPolicy(new Identity(timerId, Scope.PLAYER_QUEST), Persistence.SESSION,
			overwritePolicy, Delivery.AT_MOST_ONCE);
	}

	public record Identity(String timerId, Scope scope) {
		public Identity {
			if (timerId == null || timerId.isBlank()) {
				throw new IllegalArgumentException("timerId must not be blank");
			}
			scope = Objects.requireNonNull(scope, "scope");
		}
	}

	public enum Scope {
		PLAYER_QUEST
	}

	/** Session timers are deliberately cancelled on logout and are not restored after restart. */
	public enum Persistence {
		SESSION
	}

	public enum OverwritePolicy {
		REPLACE,
		KEEP_EXISTING,
		FAIL_IF_RUNNING
	}

	public enum Delivery {
		AT_MOST_ONCE
	}
}
