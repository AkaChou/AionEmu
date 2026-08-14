package com.aionemu.gameserver.questEngine.runtime;

import java.util.Objects;

/** 分阶段执行失败；{@code committed} 防止调用方重放耐久奖励。 / A staged execution failure; {@code committed} prevents callers from ever replaying durable rewards. */
public final class QuestExecutionFailureException extends Exception {
	private final QuestFailureStage stage;
	private final boolean committed;

	public QuestExecutionFailureException(QuestFailureStage stage, boolean committed, Throwable cause) {
		super(Objects.requireNonNull(cause, "cause").getMessage(), cause);
		this.stage = Objects.requireNonNull(stage, "stage");
		this.committed = committed;
	}

	public QuestFailureStage stage() {
		return stage;
	}

	public boolean committed() {
		return committed;
	}
}
