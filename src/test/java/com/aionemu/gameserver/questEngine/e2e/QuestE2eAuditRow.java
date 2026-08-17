package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.e2e.client.QuestTrace;

import java.util.List;
import java.util.Objects;

/**
 * 一条 transition 级任务端到端审计记录，保留独立客户端证据和运行时轨迹。
 * One transition-level quest end-to-end audit record retaining independent client evidence and the runtime trace.
 */
public record QuestE2eAuditRow(int questId, String eventType, String sourceNode, String targetNode,
		String matchedSourceNode, String matchedTargetNode, QuestE2eTransitionMatch transitionMatch,
		String validationMode,
		String targetStatus, String observedStatus, int observedPackedVariables, int npcId, int objectId,
		int dialogId, int shownPage, QuestE2eStatus status, String reason, String evidence,
		List<QuestTrace.Entry> trace) {
	public QuestE2eAuditRow {
		if (questId <= 0) throw new IllegalArgumentException("questId must be positive");
		eventType = Objects.requireNonNullElse(eventType, "");
		sourceNode = Objects.requireNonNullElse(sourceNode, "");
		targetNode = Objects.requireNonNullElse(targetNode, "");
		matchedSourceNode = Objects.requireNonNullElse(matchedSourceNode, "");
		matchedTargetNode = Objects.requireNonNullElse(matchedTargetNode, "");
		transitionMatch = Objects.requireNonNull(transitionMatch, "transitionMatch");
		validationMode = Objects.requireNonNullElse(validationMode, "FAST");
		targetStatus = Objects.requireNonNullElse(targetStatus, "");
		observedStatus = Objects.requireNonNullElse(observedStatus, "");
		status = Objects.requireNonNull(status, "status");
		reason = Objects.requireNonNullElse(reason, "");
		evidence = Objects.requireNonNullElse(evidence, "");
		trace = List.copyOf(trace);
	}
}
