package com.aionemu.gameserver.questEngine.runtime;

import java.util.Objects;

/** 数据库提交后的 best-effort 失败；可审计但绝不能触发变更重放。 / Best-effort failure after the database commit; it is auditable but must never trigger mutation replay. */
public final class QuestPostCommitFailure extends RuntimeException {
	private final QuestFailureStage stage;

	public QuestPostCommitFailure(QuestFailureStage stage, Throwable cause) {
		super(Objects.requireNonNull(cause, "cause").getMessage(), cause);
		this.stage = Objects.requireNonNull(stage, "stage");
	}

	public QuestFailureStage stage() {
		return stage;
	}
}
