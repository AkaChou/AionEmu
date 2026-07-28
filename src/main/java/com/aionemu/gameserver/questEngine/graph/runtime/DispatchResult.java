package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;

/**
 * 表示图事件分发的业务状态与候选传播决定。
 * Represents the business status and candidate propagation decision of graph event dispatch.
 */
public record DispatchResult(Status status, Propagation propagation) {

	/**
	 * 定义任务图候选评估的确定结果。
	 * Defines deterministic outcomes of quest graph candidate evaluation.
	 */
	public enum Status {
		NO_MATCH,
		APPLIED,
		REJECTED,
		FAILED
	}

	/**
	 * 定义是否继续评估后续候选。
	 * Defines whether subsequent candidates should be evaluated.
	 */
	public enum Propagation {
		CONTINUE,
		STOP
	}

	/**
	 * 拒绝缺失的状态或传播决定。
	 * Rejects a missing status or propagation decision.
	 */
	public DispatchResult {
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(propagation, "propagation");
	}
}

