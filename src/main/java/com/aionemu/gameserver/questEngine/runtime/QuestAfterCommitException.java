package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;

import java.util.Objects;

/** Auditable failure of one typed effect after the quest transaction committed. */
public final class QuestAfterCommitException extends IllegalStateException {
	private final int playerId;
	private final int questId;
	private final String actionType;

	public QuestAfterCommitException(AfterCommitAction action, QuestSnapshot snapshot) {
		this(action, snapshot, null);
	}

	public QuestAfterCommitException(AfterCommitAction action, QuestSnapshot snapshot, Throwable cause) {
		super("after-commit action " + Objects.requireNonNull(action, "action").getClass().getSimpleName()
			+ " failed for player " + Objects.requireNonNull(snapshot, "snapshot").playerId()
			+ " quest " + snapshot.questId(), cause);
		this.playerId = snapshot.playerId();
		this.questId = snapshot.questId();
		this.actionType = action.getClass().getSimpleName();
	}

	public int playerId() {
		return playerId;
	}

	public int questId() {
		return questId;
	}

	public String actionType() {
		return actionType;
	}
}
